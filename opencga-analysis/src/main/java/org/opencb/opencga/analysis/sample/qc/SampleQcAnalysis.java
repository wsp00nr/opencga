/*
 * Copyright 2015-2020 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.analysis.sample.qc;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.formats.sequence.fastqc.FastQc;
import org.opencb.biodata.formats.sequence.fastqc.io.FastQcParser;
import org.opencb.biodata.models.clinical.qc.MutationalSignature;
import org.opencb.biodata.models.clinical.qc.SampleQcVariantStats;
import org.opencb.biodata.models.clinical.qc.Signature;
import org.opencb.biodata.models.variant.metadata.SampleVariantStats;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.AnalysisUtils;
import org.opencb.opencga.analysis.individual.qc.IndividualQcUtils;
import org.opencb.opencga.analysis.tools.OpenCgaToolScopeStudy;
import org.opencb.opencga.analysis.variant.mutationalSignature.MutationalSignatureAnalysis;
import org.opencb.opencga.analysis.variant.mutationalSignature.MutationalSignatureLocalAnalysisExecutor;
import org.opencb.opencga.analysis.variant.stats.SampleVariantStatsAnalysis;
import org.opencb.opencga.analysis.wrappers.FastqcWrapperAnalysis;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.exceptions.ToolException;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.models.file.File;
import org.opencb.opencga.core.models.job.Job;
import org.opencb.opencga.core.models.sample.*;
import org.opencb.opencga.core.models.study.Study;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.core.tools.annotations.Tool;
import org.opencb.opencga.core.tools.variant.SampleQcAnalysisExecutor;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.opencb.opencga.analysis.variant.mutationalSignature.MutationalSignatureLocalAnalysisExecutor.CONTEXT_FILENAME;
import static org.opencb.opencga.core.models.study.StudyAclEntry.StudyPermissions.WRITE_SAMPLES;

@Tool(id = SampleQcAnalysis.ID, resource = Enums.Resource.SAMPLE, description = SampleQcAnalysis.DESCRIPTION)
public class SampleQcAnalysis extends OpenCgaToolScopeStudy {

    public static final String ID = "sample-qc";
    public static final String DESCRIPTION = "Run quality control (QC) for a given sample. It includes variant stats, FastQC," +
            "samtools/flagstat, picard/CollectHsMetrics and gene coverage stats; and for somatic samples, mutational signature";

    public  static final String VARIANT_STATS_STEP = "variant-stats";
    public  static final String FASTQC_STEP = "fastqc";
    public  static final String HS_METRICS_STEP = "hs-metrics";
    public  static final String FLAG_STATS_STEP = "flag-stats";
    public  static final String GENE_COVERAGE_STEP = "gene-coverage-stats";
    public  static final String MUTATIONAL_SIGNATUR_STEP = "mutational-signature";

    private String studyId;
    private String sampleId;
    private String dictFile;
    private String baitFile;
    private String variantStatsId;
    private String variantStatsDecription;
    private Query variantStatsQuery;
    private String signatureId;
    private Query signatureQuery;
    private List<String> genesForCoverageStats;

    private Sample sample;
    private File catalogBamFile;
    private SampleVariantQualityControlMetrics variantQcMetrics;
    private SampleAlignmentQualityControlMetrics alignmentQcMetrics;
    private Job variantStatsJob = null;
    private Job signatureJob = null;
    private Job fastQcJob = null;

    @Override
    protected void check() throws Exception {
        super.check();
        this.studyId = getStudyFqn();
        setUpStorageEngineExecutor(studyId);

        if (StringUtils.isEmpty(studyId)) {
            throw new ToolException("Missing study ID.");
        }

        // Check permissions
        try {
            Study study = catalogManager.getStudyManager().get(studyId, QueryOptions.empty(), token).first();
            String userId = catalogManager.getUserManager().getUserId(token);
            catalogManager.getAuthorizationManager().checkStudyPermission(study.getUid(), userId, WRITE_SAMPLES);
        } catch (CatalogException e) {
            throw new ToolException(e);
        }

        // Sanity check
        if (StringUtils.isEmpty(sampleId)) {
            throw new ToolException("Missing sample ID.");
        }

        sample = IndividualQcUtils.getValidSampleById(studyId, sampleId, catalogManager, token);
        if (sample == null) {
            throw new ToolException("Sample '" + sampleId + "' not found.");
        }

        try {
            catalogBamFile = AnalysisUtils.getBamFileBySampleId(sample.getId(), studyId, catalogManager.getFileManager(), getToken());
        } catch (ToolException e) {
            throw new ToolException(e);
        }
    }

    @Override
    protected List<String> getSteps() {
        return Arrays.asList(VARIANT_STATS_STEP, FASTQC_STEP, FLAG_STATS_STEP, HS_METRICS_STEP, GENE_COVERAGE_STEP,
                MUTATIONAL_SIGNATUR_STEP);
    }

    @Override
    protected void run() throws ToolException {

        // Get job dependencies
        try {
            OpenCGAResult<Job> jobResult = catalogManager.getJobManager().get(studyId, getJobId(), QueryOptions.empty(), token);
            Job job = jobResult.first();
            if (CollectionUtils.isNotEmpty(job.getDependsOn())) {
                for (Job dependsOnJob : job.getDependsOn()) {
                    if (dependsOnJob.getId().startsWith(SampleVariantStatsAnalysis.ID)) {
                        variantStatsJob = catalogManager.getJobManager().get(studyId, dependsOnJob.getId(), QueryOptions.empty(), token)
                                .first();
                    } else if (dependsOnJob.getId().startsWith(MutationalSignatureAnalysis.ID)) {
                        signatureJob = catalogManager.getJobManager().get(studyId, dependsOnJob.getId(), QueryOptions.empty(), token)
                                .first();
                    } else if (dependsOnJob.getId().startsWith(FastqcWrapperAnalysis.ID)) {
                        fastQcJob = catalogManager.getJobManager().get(studyId, dependsOnJob.getId(), QueryOptions.empty(), token)
                                .first();
                    }
                }
            }
        } catch (CatalogException e) {
            throw new ToolException(e);
        }

        // Get sample quality control metrics to update
        alignmentQcMetrics = getSampleAlignmentQualityControlMetrics();
        variantQcMetrics = sample.getQualityControl().getVariantMetrics();

        SampleQcAnalysisExecutor executor = getToolExecutor(SampleQcAnalysisExecutor.class);

        // Set up executor
        executor.setStudyId(studyId)
                .setSample(sample)
                .setCatalogBamFile(catalogBamFile)
                .setDictFile(dictFile)
                .setBaitFile(baitFile)
                .setVariantStatsId(variantStatsId)
                .setVariantStatsDecription(variantStatsDecription)
                .setVariantStatsQuery(variantStatsQuery)
                .setSignatureId(signatureId)
                .setSignatureQuery(signatureQuery)
                .setGenesForCoverageStats(genesForCoverageStats)
                .setAlignmentQcMetrics(alignmentQcMetrics);

        // Step by step
        step(VARIANT_STATS_STEP, () -> runVariantStats());
        step(FASTQC_STEP, () -> runFastQc());//executor.setQcType(SampleQcAnalysisExecutor.QcType.FASTQC).execute());
        step(FLAG_STATS_STEP, () -> executor.setQcType(SampleQcAnalysisExecutor.QcType.FLAG_STATS).execute());
        step(HS_METRICS_STEP, () -> executor.setQcType(SampleQcAnalysisExecutor.QcType.HS_METRICS).execute());
        step(GENE_COVERAGE_STEP, () -> executor.setQcType(SampleQcAnalysisExecutor.QcType.GENE_COVERAGE_STATS).execute());
        step(MUTATIONAL_SIGNATUR_STEP, () -> runSignature());

        // Finally, update sample quality control metrics
        alignmentQcMetrics = executor.getAlignmentQcMetrics();
        updateSampleQualityControlMetrics(alignmentQcMetrics);
    }

    private void runVariantStats() throws ToolException {
        if (variantStatsJob == null) {
            addWarning("Skipping sample variant stats");
        } else {
            Path path = Paths.get(variantStatsJob.getOutDir().getUri().getPath()).resolve("sample-variant-stats.json");
            if (path.toFile().exists()) {
                if (variantQcMetrics.getVariantStats() == null) {
                    variantQcMetrics.setVariantStats(new ArrayList<>());
                }

                List<SampleVariantStats> stats = new ArrayList<>();
                try {
                    JacksonUtils.getDefaultObjectMapper()
                            .readerFor(SampleVariantStats.class)
                            .<SampleVariantStats>readValues(path.toFile())
                            .forEachRemaining(stats::add);
                } catch (IOException e) {
                    throw new ToolException(e);
                }

                // Convert variant stats query to a map, and then add to metrics
                Map<String, String> query = new HashMap<>();
                Iterator<String> iterator = variantStatsQuery.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    query.put(key, variantStatsQuery.getString(key));
                }

                variantQcMetrics.getVariantStats().add(new SampleQcVariantStats(variantStatsId, variantStatsDecription, query, stats.get(0)));
            }
        }
    }

    private void runSignature() throws ToolException {
        if (!sample.isSomatic()) {
            addWarning("Skipping mutational signature: sample '" + sampleId + " is not somatic");
            return;
        }

        // mutationalSignature/run
        if (signatureJob != null) {
            Path path = Paths.get(signatureJob.getOutDir().getUri().getPath()).resolve(CONTEXT_FILENAME);
            if (path.toFile().exists()) {
                Signature.SignatureCount[] signatureCounts = MutationalSignatureAnalysis.parseSignatureCounts(path.toFile());

                // Add to metrics
                variantQcMetrics.getSignatures().add(new Signature("ALL", new HashMap<>(), "SNV", signatureCounts, new ArrayList()));
            }
        }

        // mutationalSignature/query
        if (StringUtils.isNotEmpty(signatureId) && signatureQuery != null && !signatureQuery.isEmpty()) {
            // Create signature directory
            Path signaturePath = getOutDir().resolve("mutational-signature");
            signaturePath.toFile().mkdir();

            MutationalSignatureLocalAnalysisExecutor executor = new MutationalSignatureLocalAnalysisExecutor();
            ObjectMap executorParams = new ObjectMap();
            executorParams.put("opencgaHome", getOpencgaHome().toString());
            executorParams.put("token", token);
            executorParams.put("fitting", false);
            executor.setUp(null, executorParams, signaturePath);
            executor.setStudy(studyId);
            executor.setSampleName(sampleId);

            try {
                signatureQuery.put("study", studyId);
                signatureQuery.put("sample", sampleId);
                MutationalSignature mutationalSignature = executor.query(signatureQuery, QueryOptions.empty());

                // Add signature to metrics
                Map<String, String> map = new HashMap<>();
                for (String key : signatureQuery.keySet()) {
                    map.put(key, signatureQuery.getString(key));
                }
                variantQcMetrics.getSignatures().add(new Signature(signatureId, map, "SNV", mutationalSignature.getSignature().getCounts(),
                        new ArrayList()));
            } catch (CatalogException | StorageEngineException | IOException e) {
                throw new ToolException("Error computing mutational signature for query '" + signatureId + "'", e);
            }
        } else {
            addWarning("Skipping mutational signature: invalid parameters signature ID ('" + signatureId + "') and signature query('"
                    + signatureQuery + "')");
        }
    }


    private void runFastQc() throws ToolException {
        if (fastQcJob == null) {
            addWarning("Skipping FastQc analysis");
            return;
        }

        if (CollectionUtils.isNotEmpty(fastQcJob.getOutput())) {
            FastQc fastQc = null;

            // First, look for fastqc_data.txt to parse it
            for (File file: fastQcJob.getOutput()) {
                if (file.getName().equals("fastqc_data.txt")) {
                    try {
                        fastQc = FastQcParser.parse(Paths.get(file.getUri().getPath()).toFile());
                    } catch (IOException e) {
                        throw new ToolException(e);
                    }
                }
            }

            // Second, add images to the FastQc object
            if (fastQc != null) {
                for (File file : fastQcJob.getOutput()) {
                    switch (file.getName()) {
                        case "adapter_content.png": {
                            fastQc.getAdapterContent().setFile(file.getId());
                            break;
                        }
                        case "per_base_n_content.png": {
                            fastQc.getPerBaseNContent().setFile(file.getId());
                            break;
                        }
                        case "per_base_sequence_content.png": {
                            fastQc.getPerBaseSeqContent().setFile(file.getId());
                            break;
                        }
                        case "per_sequence_quality.png": {
                            fastQc.getPerSeqQualityScore().setFile(file.getId());
                            break;
                        }
                        case "duplication_levels.png": {
                            fastQc.getSeqDuplicationLevel().setFile(file.getId());
                            break;
                        }
                        case "per_base_quality.png": {
                            fastQc.getPerBaseSeqQuality().setFile(file.getId());
                            break;
                        }
                        case "per_sequence_gc_content.png": {
                            fastQc.getPerSeqGcContent().setFile(file.getId());
                            break;
                        }
                        case "sequence_length_distribution.png": {
                            fastQc.getSeqLengthDistribution().setFile(file.getId());
                            break;
                        }
                    }
                }

                alignmentQcMetrics.setFastQc(fastQc);
            }
        }
    }

    private SampleAlignmentQualityControlMetrics getSampleAlignmentQualityControlMetrics() {
        String bamFileId = (catalogBamFile == null) ? "" : catalogBamFile.getId();

        if (sample.getQualityControl() == null) {
            sample.setQualityControl(new SampleQualityControl());
        } else {
            for (SampleAlignmentQualityControlMetrics prevMetrics : sample.getQualityControl().getAlignmentMetrics()) {
                if (prevMetrics.getBamFileId().equals(bamFileId)) {
                    return prevMetrics;
                }
            }
        }
        return new SampleAlignmentQualityControlMetrics().setBamFileId(bamFileId);
    }

    private void updateSampleQualityControlMetrics(SampleAlignmentQualityControlMetrics alignmentQcMetrics) throws ToolException {
        SampleQualityControl qualityControl = sample.getQualityControl();
        if (sample.getQualityControl() == null) {
            qualityControl = new SampleQualityControl();
        } else {
            int index = 0;
            for (SampleAlignmentQualityControlMetrics prevMetrics : qualityControl.getAlignmentMetrics()) {
                if (prevMetrics.getBamFileId().equals(alignmentQcMetrics.getBamFileId())) {
                    qualityControl.getAlignmentMetrics().remove(index);
                    break;
                }
                index++;
            }
        }

        try {
            catalogManager.getSampleManager().update(studyId, getSampleId(), new SampleUpdateParams().setQualityControl(qualityControl),
                    QueryOptions.empty(), token);
        } catch (CatalogException e) {
            throw new ToolException(e);
        }
    }

    public SampleQcAnalysis setStudyId(String studyId) {
        super.setStudy(studyId);
        return this;
    }

    public String getSampleId() {
        return sampleId;
    }

    public SampleQcAnalysis setSampleId(String sampleId) {
        this.sampleId = sampleId;
        return this;
    }

    public String getDictFile() {
        return dictFile;
    }

    public SampleQcAnalysis setDictFile(String dictFile) {
        this.dictFile = dictFile;
        return this;
    }

    public String getBaitFile() {
        return baitFile;
    }

    public SampleQcAnalysis setBaitFile(String baitFile) {
        this.baitFile = baitFile;
        return this;
    }

    public String getVariantStatsId() {
        return variantStatsId;
    }

    public SampleQcAnalysis setVariantStatsId(String variantStatsId) {
        this.variantStatsId = variantStatsId;
        return this;
    }

    public String getVariantStatsDecription() {
        return variantStatsDecription;
    }

    public SampleQcAnalysis setVariantStatsDecription(String variantStatsDecription) {
        this.variantStatsDecription = variantStatsDecription;
        return this;
    }

    public Query getVariantStatsQuery() {
        return variantStatsQuery;
    }

    public SampleQcAnalysis setVariantStatsQuery(Query variantStatsQuery) {
        this.variantStatsQuery = variantStatsQuery;
        return this;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public SampleQcAnalysis setSignatureId(String signatureId) {
        this.signatureId = signatureId;
        return this;
    }

    public Query getSignatureQuery() {
        return signatureQuery;
    }

    public SampleQcAnalysis setSignatureQuery(Query signatureQuery) {
        this.signatureQuery = signatureQuery;
        return this;
    }

    public List<String> getGenesForCoverageStats() {
        return genesForCoverageStats;
    }

    public SampleQcAnalysis setGenesForCoverageStats(List<String> genesForCoverageStats) {
        this.genesForCoverageStats = genesForCoverageStats;
        return this;
    }
}
