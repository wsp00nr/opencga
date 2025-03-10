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

package org.opencb.opencga.analysis.variant.manager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.opencb.biodata.models.core.Region;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.avro.FileEntry;
import org.opencb.biodata.models.variant.avro.SampleEntry;
import org.opencb.biodata.models.variant.avro.VariantAnnotation;
import org.opencb.biodata.models.variant.avro.VariantType;
import org.opencb.biodata.models.variant.metadata.SampleVariantStats;
import org.opencb.biodata.models.variant.metadata.VariantMetadata;
import org.opencb.biodata.tools.variant.converters.ga4gh.Ga4ghVariantConverter;
import org.opencb.biodata.tools.variant.converters.ga4gh.factories.AvroGa4GhVariantFactory;
import org.opencb.biodata.tools.variant.converters.ga4gh.factories.ProtoGa4GhVariantFactory;
import org.opencb.commons.datastore.core.*;
import org.opencb.commons.datastore.core.result.Error;
import org.opencb.commons.datastore.solr.SolrManager;
import org.opencb.opencga.analysis.StorageManager;
import org.opencb.opencga.analysis.variant.VariantExportTool;
import org.opencb.opencga.analysis.variant.manager.operations.*;
import org.opencb.opencga.analysis.variant.metadata.CatalogStorageMetadataSynchronizer;
import org.opencb.opencga.analysis.variant.metadata.CatalogVariantMetadataFactory;
import org.opencb.opencga.analysis.variant.operations.*;
import org.opencb.opencga.analysis.variant.stats.VariantStatsAnalysis;
import org.opencb.opencga.catalog.audit.AuditRecord;
import org.opencb.opencga.catalog.db.api.*;
import org.opencb.opencga.catalog.exceptions.CatalogAuthorizationException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.managers.StudyManager;
import org.opencb.opencga.core.common.UriUtils;
import org.opencb.opencga.core.models.cohort.Cohort;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.models.family.Family;
import org.opencb.opencga.core.models.file.File;
import org.opencb.opencga.core.models.individual.Individual;
import org.opencb.opencga.core.models.project.DataStore;
import org.opencb.opencga.core.models.project.Project;
import org.opencb.opencga.core.models.sample.Sample;
import org.opencb.opencga.core.models.sample.SampleAclEntry;
import org.opencb.opencga.core.models.study.Study;
import org.opencb.opencga.core.models.study.StudyAclEntry;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.core.response.VariantQueryResult;
import org.opencb.opencga.storage.core.StorageEngineFactory;
import org.opencb.opencga.storage.core.StoragePipelineResult;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.metadata.VariantMetadataFactory;
import org.opencb.opencga.storage.core.metadata.VariantStorageMetadataManager;
import org.opencb.opencga.storage.core.metadata.models.ProjectMetadata;
import org.opencb.opencga.storage.core.metadata.models.SampleMetadata;
import org.opencb.opencga.storage.core.metadata.models.StudyMetadata;
import org.opencb.opencga.storage.core.metadata.models.VariantScoreMetadata;
import org.opencb.opencga.storage.core.utils.CellBaseUtils;
import org.opencb.opencga.storage.core.variant.BeaconResponse;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;
import org.opencb.opencga.storage.core.variant.adaptors.VariantField;
import org.opencb.opencga.storage.core.variant.adaptors.VariantIterable;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.core.variant.adaptors.iterators.VariantDBIterator;
import org.opencb.opencga.storage.core.variant.io.VariantWriterFactory.VariantOutputFormat;
import org.opencb.opencga.storage.core.variant.query.ParsedQuery;
import org.opencb.opencga.storage.core.variant.query.VariantQueryUtils;
import org.opencb.opencga.storage.core.variant.query.projection.VariantQueryProjectionParser;
import org.opencb.opencga.storage.core.variant.score.VariantScoreFormatDescriptor;
import org.opencb.opencga.storage.core.variant.search.solr.VariantSearchLoadResult;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.opencb.commons.datastore.core.QueryOptions.*;
import static org.opencb.opencga.analysis.variant.manager.operations.VariantFileIndexerOperationManager.FILE_GET_QUERY_OPTIONS;
import static org.opencb.opencga.core.api.ParamConstants.ACL_PARAM;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.*;
import static org.opencb.opencga.storage.core.variant.adaptors.sample.VariantSampleDataManager.SAMPLE_BATCH_SIZE;
import static org.opencb.opencga.storage.core.variant.adaptors.sample.VariantSampleDataManager.SAMPLE_BATCH_SIZE_DEFAULT;
import static org.opencb.opencga.storage.core.variant.query.VariantQueryUtils.*;

public class VariantStorageManager extends StorageManager implements AutoCloseable {

    private final VariantCatalogQueryUtils catalogUtils;

    public VariantStorageManager(CatalogManager catalogManager, StorageEngineFactory storageEngineFactory) {
        super(catalogManager, storageEngineFactory);
        catalogUtils = new VariantCatalogQueryUtils(catalogManager);
    }

    public void clearCache(String studyId, String type, String token) throws CatalogException {
        String userId = catalogManager.getUserManager().getUserId(token);

    }

    // -------------------------//
    //   Import/Export methods  //
    // -------------------------//

    /**
     * Loads the given file into an empty study.
     * <p>
     * The input file should have, in the same directory, a metadata file, with the same name ended with
     * {@link org.opencb.opencga.storage.core.variant.io.VariantExporter#METADATA_FILE_EXTENSION}
     *
     * @param inputUri  Variants input file in avro format.
     * @param study     Study where to load the variants
     * @param token User's session id
     * @throws CatalogException, StorageEngineException      if there is any error importing
     */
    public void importData(URI inputUri, String study, String token)
            throws CatalogException, StorageEngineException {

        secureOperation("variant-import", study, new ObjectMap(), token, engine -> {
            new VariantImportOperationManager(this, engine).run(study, inputUri, token);
            return null;
        });
    }

    /**
     * Exports the result of the given query and the associated metadata.
     *
     * @param outputFile   Optional output file. If null or empty, will print into the Standard output. Won't export any metadata.
     * @param outputFormat Output format.
     * @param study        Study to export
     * @param token    User's session id
     * @throws CatalogException       if there is any error with Catalog
     * @throws IOException            If there is any IO error
     * @throws StorageEngineException If there is any error exporting variants
     */
    public void exportData(String outputFile, VariantOutputFormat outputFormat, String study, String token)
            throws StorageEngineException, CatalogException, IOException {
        Query query = new Query(VariantQueryParam.INCLUDE_STUDY.key(), study)
                .append(VariantQueryParam.STUDY.key(), study);
        exportData(outputFile, outputFormat, null, query, new QueryOptions(), token);
    }

    /**
     * Exports the result of the given query and the associated metadata.
     * @param outputFile    Optional output file. If null or empty, will print into the Standard output. Won't export any metadata.
     * @param outputFormat  Variant Output format.
     * @param variantsFile  Optional variants file.
     * @param query         Query with the variants to export
     * @param queryOptions  Query options
     * @param token         User's session id
     * @throws CatalogException if there is any error with Catalog
     * @throws IOException  If there is any IO error
     * @throws StorageEngineException  If there is any error exporting variants
     */
    public void exportData(String outputFile, VariantOutputFormat outputFormat, String variantsFile,
                           Query query, QueryOptions queryOptions, String token)
            throws CatalogException, IOException, StorageEngineException {
        catalogUtils.parseQuery(query, token);
        checkSamplesPermissions(query, queryOptions, token);

        secureOperation(VariantExportTool.ID, catalogUtils.getAnyStudy(query, token), queryOptions, token, engine -> {
            new VariantExportOperationManager(this, engine).export(outputFile, outputFormat, variantsFile, query, queryOptions, token);
            return null;
        });
    }

    // --------------------------//
    //   Data Operation methods  //
    // --------------------------//

    public List<StoragePipelineResult> index(String study, String fileId, String outDir, ObjectMap config, String token)
            throws CatalogException, StorageEngineException {
        return index(study, Arrays.asList(StringUtils.split(fileId, ",")), outDir, config, token);
    }

    public List<StoragePipelineResult> index(String study, List<String> files, String outDir, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        return secureOperation(VariantIndexOperationTool.ID, study, params, token, engine ->
                new VariantFileIndexerOperationManager(this, engine)
                        .index(study, files, UriUtils.createDirectoryUriSafe(outDir), params, token));
    }

    public void secondaryIndexSamples(String study, List<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantSecondaryIndexSamplesOperationTool.ID, study, params, token, engine -> {
            engine.secondaryIndexSamples(study, samples);
            return null;
        });
    }

    public void removeSearchIndexSamples(String study, List<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation("removeSecondaryIndexSamples", study, params, token, engine -> {
            engine.removeSecondaryIndexSamples(study, samples);
            return null;
        });
    }

    public VariantSearchLoadResult secondaryIndex(String project, String region, boolean overwrite, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        return secureOperationByProject(VariantSecondaryIndexOperationTool.ID, project, params, token, engine -> {
            Query inputQuery = new Query();
            inputQuery.putIfNotEmpty(VariantQueryParam.REGION.key(), region);
            return engine.secondaryIndex(inputQuery, new QueryOptions(params), overwrite);
        });
    }

    public void removeStudy(String study, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantFileDeleteOperationTool.ID, study, params, token, engine -> {
            new VariantFileDeleteOperationManager(this, engine).removeStudy(getStudyFqn(study, token), token);
            return null;
        });
    }

    public void removeFile(String study, List<String> files, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantFileDeleteOperationTool.ID, study, params, token, engine -> {
            new VariantFileDeleteOperationManager(this, engine).removeFile(study, files, token);
            return null;
        });
    }

    public void annotate(String study, String region, String outDir, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        annotate(null, Arrays.asList(StringUtils.split(study, ",")), region, false, outDir, null, params, token);
    }

    public void annotationLoad(String projectStr, List<String> studies, String loadFile, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        String projectId = getProjectId(projectStr, studies, token);
        secureOperation(VariantAnnotationIndexOperationTool.ID, projectId, studies, params, token, engine -> {
            new VariantAnnotationOperationManager(this, engine)
                    .annotationLoad(projectStr, getStudiesFqn(studies, token), params, loadFile, token);
            return null;
        });
    }

    public void annotate(String projectStr, List<String> studies, String region, boolean overwriteAnnotations, String outDir,
                         String outputFileName, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        String projectId = getProjectId(projectStr, studies, token);
        secureOperationByProject(VariantAnnotationIndexOperationTool.ID, projectId, params, token, engine -> {
            List<String> studiesFqn = getStudiesFqn(studies, token);
            new VariantAnnotationOperationManager(this, engine)
                    .annotate(projectId, studiesFqn, region, outputFileName, Paths.get(outDir), params, token, overwriteAnnotations);
            return null;
        });
    }

    public void saveAnnotation(String project, String annotationName, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperationByProject(VariantAnnotationSaveOperationTool.ID, project, params, token, engine -> {
            CatalogStorageMetadataSynchronizer
                    .updateProjectMetadata(catalogManager, engine.getMetadataManager(), project, token);
            engine.saveAnnotation(annotationName, params);
            return null;
        });
    }

    public void deleteAnnotation(String project, String annotationName, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperationByProject(VariantAnnotationDeleteOperationTool.ID, project, params, token, engine -> {
            CatalogStorageMetadataSynchronizer
                    .updateProjectMetadata(catalogManager, engine.getMetadataManager(), project, token);
            engine.deleteAnnotation(annotationName, params);
            return null;
        });
    }

    public DataResult<VariantAnnotation> getAnnotation(String name, Query query, QueryOptions options, String token)
            throws StorageEngineException, CatalogException {
        QueryOptions finalOptions = VariantQueryUtils.validateAnnotationQueryOptions(options);
        return secure(query, finalOptions, token, (engine) -> engine.getAnnotation(name, query, finalOptions));
    }

    public DataResult<ProjectMetadata.VariantAnnotationMetadata> getAnnotationMetadata(String name, String project, String token)
            throws StorageEngineException, CatalogException {
        Query query = new Query(VariantCatalogQueryUtils.PROJECT.key(), project);
        return secure(query, empty(), token, (engine) -> engine.getAnnotationMetadata(name));
    }

    public void stats(String study, List<String> cohorts, String region, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {

        secureOperation(VariantStatsAnalysis.ID, study, params, token, engine -> {
            new VariantStatsOperationManager(this, engine).stats(getStudyFqn(study, token), cohorts, region, params, token);
            return null;
        });
    }

    public void deleteStats(List<String> cohorts, String studyId, String token) {
        throw new UnsupportedOperationException();
    }

    public List<VariantScoreMetadata> listVariantScores(String study, String token) throws CatalogException, StorageEngineException {
        String studyFqn = getStudyFqn(study, token);
        DataStore dataStore = getDataStore(studyFqn, token);
        VariantStorageEngine engine = getVariantStorageEngine(dataStore);

        return engine.getMetadataManager().getStudyMetadata(studyFqn).getVariantScores();
    }

    public void variantScoreLoad(String study, URI scoreFile, String scoreName, String cohort1, String cohort2,
                                 VariantScoreFormatDescriptor descriptor, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        descriptor.checkValid();
        String studyFqn = getStudyFqn(study, token);
        secureOperation(VariantScoreIndexOperationTool.ID, study, params, token, engine -> {
            String cohort1Id;
            String cohort2Id;

            Cohort cohort1Obj = catalogManager.getCohortManager().get(study, cohort1, new QueryOptions(), token).first();
            List<String> cohort1Samples = cohort1Obj.getSamples().stream().map(Sample::getId).collect(Collectors.toList());
            engine.getMetadataManager().registerCohort(studyFqn, cohort1Obj.getId(), cohort1Samples);
            cohort1Id = cohort1Obj.getId();

            if (StringUtils.isNotEmpty(cohort2)) {
                Cohort cohort2Obj = catalogManager.getCohortManager().get(study, cohort2, new QueryOptions(), token).first();
                List<String> cohort2Samples = cohort2Obj.getSamples().stream().map(Sample::getId).collect(Collectors.toList());
                engine.getMetadataManager().registerCohort(studyFqn, cohort2Obj.getId(), cohort2Samples);
                cohort2Id = cohort2Obj.getId();
            } else {
                cohort2Id = null;
            }

            engine.loadVariantScore(scoreFile, studyFqn, scoreName, cohort1Id, cohort2Id, descriptor, params);
            return null;
        });

    }

    public void variantScoreDelete(String study, String scoreName, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantScoreDeleteOperationTool.ID, study, params, token, engine -> {
            engine.deleteVariantScore(getStudyFqn(study, token), scoreName, params);
            return null;
        });
    }

    public void sampleIndex(String study, List<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantSampleIndexOperationTool.ID, study, params, token, engine -> {
            engine.sampleIndex(getStudyFqn(study, token), samples, params);
            return null;
        });
    }

    public void sampleIndexAnnotate(String study, List<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantSampleIndexOperationTool.ID, study, params, token, engine -> {
            engine.sampleIndexAnnotate(getStudyFqn(study, token), samples, params);
            return null;
        });
    }

    public DataResult<List<String>> familyIndex(String study, List<String> familiesStr, boolean skipIncompleteFamilies,
                                                ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        return secureOperation(VariantFamilyIndexOperationTool.ID, study, params, token, engine -> {
            List<List<String>> trios = new LinkedList<>();
            List<Event> events = new LinkedList<>();
            VariantStorageMetadataManager metadataManager = engine.getMetadataManager();
            VariantCatalogQueryUtils catalogUtils = new VariantCatalogQueryUtils(catalogManager);
            if (familiesStr.size() == 1 && familiesStr.get(0).equals(VariantQueryUtils.ALL)) {
                DBIterator<Family> iterator = catalogManager.getFamilyManager().iterator(study, new Query(), new QueryOptions(), token);
                while (iterator.hasNext()) {
                    Family family = iterator.next();
                    trios.addAll(catalogUtils.getTriosFromFamily(study, family, metadataManager, true, token));
                }
            } else {
                for (String familyId : familiesStr) {
                    Family family = catalogManager.getFamilyManager().get(study, familyId, null, token).first();
                    trios.addAll(catalogUtils.getTriosFromFamily(study, family, metadataManager, skipIncompleteFamilies, token));
                }
            }

            return engine.familyIndex(study, trios, params);
        });
    }

    public DataResult<List<String>> familyIndexBySamples(String study, Collection<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        return secureOperation(VariantFamilyIndexOperationTool.ID, study, params, token, engine -> {

            OpenCGAResult<Individual> individualResult = getCatalogManager().getIndividualManager()
                    .search(study,
                            new Query(IndividualDBAdaptor.QueryParams.SAMPLES.key(), samples),
                            new QueryOptions(), token);

            List<List<String>> trios = catalogUtils.getTrios(study, engine.getMetadataManager(), individualResult.getResults(), token);

            return engine.familyIndex(study, trios, params);
        });
    }

    public List<List<String>> getTriosFromFamily(String study, Family family, boolean skipIncompleteFamilies, String token)
            throws CatalogException, StorageEngineException {
        VariantStorageEngine variantStorageEngine = getVariantStorageEngine(study, token);
        return catalogUtils.getTriosFromFamily(study, family, variantStorageEngine.getMetadataManager(), skipIncompleteFamilies, token);
    }

    public void aggregateFamily(String studyStr, List<String> samples, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {

        secureOperation(VariantAggregateFamilyOperationTool.ID, studyStr, params, token, engine -> {
            engine.aggregateFamily(getStudyFqn(studyStr, token), samples, params);
            return null;
        });
    }

    public void aggregate(String studyStr, boolean overwrite, ObjectMap params, String token)
            throws CatalogException, StorageEngineException {
        secureOperation(VariantAggregateOperationTool.ID, studyStr, params, token, engine -> {
            engine.aggregate(getStudyFqn(studyStr, token), overwrite, params);
            return null;
        });
    }

    public ObjectMap configureProject(String projectStr, ObjectMap params, String token) throws CatalogException, StorageEngineException {
        return secureOperationByProject("configure", projectStr, params, token, engine -> {
            DataStore dataStore = getDataStoreByProjectId(projectStr, token);

            dataStore.getConfiguration().putAll(params);
            catalogManager.getProjectManager()
                    .update(projectStr, new ObjectMap(ProjectDBAdaptor.QueryParams.INTERNAL_DATASTORES_VARIANT.key(), dataStore),
                            new QueryOptions(), token);
            return dataStore.getConfiguration();
        });
    }

//    public void configureStudy(String studyStr, ObjectMap params, String token) throws CatalogException, StorageEngineException {
//        secureOperation("configure", studyStr, params, token, engine -> {
//            String studyFqn = getStudyFqn(studyStr, token);
//            engine.getConfigurationManager().configureStudy(studyFqn, params);
//            return null;
//        });
//    }


    // ---------------------//
    //   Query methods      //
    // ---------------------//

    public VariantQueryResult<Variant> get(Query inputQuery, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException, IOException {
        Query query = inputQuery == null ? new Query() : new Query(inputQuery);
        return secure(query, queryOptions, token, Enums.Action.SEARCH, engine -> {
            logger.debug("getVariants {}, {}", query, queryOptions);
            VariantQueryResult<Variant> result = engine.get(query, queryOptions);
            logger.debug("gotVariants {}, {}, in {}ms", result.getNumResults(), result.getNumMatches(), result.getTime());
            return result;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> VariantQueryResult<T> get(Query query, QueryOptions queryOptions, String token, Class<T> clazz)
            throws CatalogException, IOException, StorageEngineException {
        VariantQueryResult<Variant> result = get(query, queryOptions, token);
        List<T> variants;
        if (clazz == Variant.class) {
            return (VariantQueryResult<T>) result;
        } else if (clazz == org.ga4gh.models.Variant.class) {
            Ga4ghVariantConverter<org.ga4gh.models.Variant> converter = new Ga4ghVariantConverter<>(new AvroGa4GhVariantFactory());
            variants = (List<T>) converter.apply(result.getResults());
        } else if (clazz == ga4gh.Variants.Variant.class) {
            Ga4ghVariantConverter<ga4gh.Variants.Variant> converter = new Ga4ghVariantConverter<>(new ProtoGa4GhVariantFactory());
            variants = (List<T>) converter.apply(result.getResults());
        } else {
            throw new IllegalArgumentException("Unknown variant format " + clazz);
        }
        return new VariantQueryResult<>(
                result.getTime(),
                result.getNumResults(),
                result.getNumMatches(),
                result.getEvents(),
                variants,
                result.getSamples(),
                result.getSource(),
                result.getApproximateCount(),
                result.getApproximateCountSamplingSize(), null);

    }

    public DataResult<VariantMetadata> getMetadata(Query query, QueryOptions queryOptions, String token)
            throws CatalogException, IOException, StorageEngineException {
        return secure(query, queryOptions, token, engine -> {
            StopWatch watch = StopWatch.createStarted();
            VariantMetadataFactory metadataFactory = new CatalogVariantMetadataFactory(catalogManager, engine.getDBAdaptor(), token);
            addDefaultSampleLimit(query, engine.getOptions());
            VariantMetadata metadata = metadataFactory.makeVariantMetadata(query, queryOptions);
            return new DataResult<>(((int) watch.getTime()), Collections.emptyList(), 1, Collections.singletonList(metadata), 1);
        });
    }

    //TODO: GroupByFieldEnum
    public DataResult groupBy(String field, Query query, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException, IOException {
        return (DataResult) secure(query, queryOptions, token, engine -> engine.groupBy(query, field, queryOptions));
    }

    public DataResult rank(Query query, String field, int limit, boolean asc, String token)
            throws StorageEngineException, CatalogException, IOException {
        int limitMax = 30;
        int limitDefault = 10;
        return (DataResult) secure(query, null, token,
                engine -> engine.rank(query, field, (limit > 0) ? Math.min(limit, limitMax) : limitDefault, asc));
    }

    public DataResult<Long> count(Query query, String token) throws CatalogException, StorageEngineException, IOException {
        return secure(query, new QueryOptions(QueryOptions.EXCLUDE, VariantField.STUDIES), token,
                engine -> engine.count(query));
    }

    public DataResult distinct(Query query, String field, String token)
            throws CatalogException, IOException, StorageEngineException {
        return (DataResult) secure(query, new QueryOptions(QueryOptions.EXCLUDE, VariantField.STUDIES), token,
                engine -> engine.distinct(query, field));
    }

    public VariantQueryResult<Variant> getPhased(Variant variant, String study, String sample, String token, QueryOptions options)
            throws CatalogException, IOException, StorageEngineException {
        return secure(new Query(VariantQueryParam.STUDY.key(), study), options, token,
                engine -> engine.getPhased(variant.toString(), study, sample, options, 5000));
    }

    public DataResult getFrequency(Query query, int interval, String token)
            throws CatalogException, IOException, StorageEngineException {
        return (DataResult) secure(query, null, token, engine -> {
            String[] regions = getRegions(query);
            if (regions.length != 1) {
                throw new IllegalArgumentException("Unable to calculate histogram with " + regions.length + " regions.");
            }
            return engine.getFrequency(query, Region.parseRegion(regions[0]), interval);
        });
    }

    public VariantIterable iterable(String token) {
        return (query, options) -> {
            try {
                return iterator(query, options, token);
            } catch (CatalogException | StorageEngineException e) {
                throw new VariantQueryException("Error getting variant iterator", e);
            }
        };
    }

    public VariantDBIterator iterator(String token) throws CatalogException, StorageEngineException {
        return iterator(null, null, token);
    }

    public VariantDBIterator iterator(Query query, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException {
        String study = catalogUtils.getAnyStudy(query, token);

        DataStore dataStore = getDataStore(study, token);
        VariantStorageEngine storageEngine = getVariantStorageEngine(dataStore);
        catalogUtils.parseQuery(query, token);
        checkSamplesPermissions(query, queryOptions, storageEngine.getMetadataManager(), token);
        return storageEngine.iterator(query, queryOptions);
    }

//    public <T> VariantDBIterator<T> iterator(Query query, QueryOptions queryOptions, Class<T> clazz, String token) {
//        return null;
//    }

    public VariantQueryResult<Variant> intersect(Query query, QueryOptions queryOptions, List<String> studyIds, String token)
            throws CatalogException, IOException, StorageEngineException {
        Query intersectQuery = new Query(query);
        intersectQuery.put(VariantQueryParam.STUDY.key(), String.join(VariantQueryUtils.AND, studyIds));
        return get(intersectQuery, queryOptions, token);
    }

    public DataResult<SampleVariantStats> getSampleStats(String studyStr, String sample, Query inputQuery, String token)
            throws CatalogException, IOException, StorageEngineException {
        Query query = inputQuery == null ? new Query() : new Query(inputQuery);
        query.put(STUDY.key(), studyStr);
        query.put(SAMPLE.key(), sample);

       return secure(query, new QueryOptions(), token, Enums.Action.FACET, engine -> {
            logger.debug("getSampleStats {}", query);
            DataResult<SampleVariantStats> result = engine.sampleStatsQuery(
                    query.getString(STUDY.key()),
                    query.getString(SAMPLE.key()),
                    query);
            logger.debug("getFacets in {}ms", result.getTime());
            return result;
        });
    }

    public DataResult<Variant> getSampleData(String variant, String study, QueryOptions inputOptions, String token)
            throws CatalogException, IOException, StorageEngineException {
        QueryOptions options = inputOptions == null ? new QueryOptions() : new QueryOptions(inputOptions);
        options.remove(QueryOptions.INCLUDE);
        options.remove(QueryOptions.EXCLUDE);
        options.remove(VariantField.SUMMARY);
        Query query = new Query(options)
                .append(VariantQueryParam.STUDY.key(), study);
        query.remove(GENOTYPE.key());

        if (isValidParam(query, INCLUDE_SAMPLE) && !VariantQueryUtils.isAll(query.getString(INCLUDE_SAMPLE.key()))) {
            return secure(query, options, token, Enums.Action.SAMPLE_DATA, engine -> {
                String studyFqn = query.getString(STUDY.key());
                options.putAll(query);
                return engine.getSampleData(variant, studyFqn, options);
            });
        } else {
            // Skip sample permission check. Run check with results
            options.put(VariantField.SUMMARY, true);
            return secure(query, options, token, Enums.Action.SAMPLE_DATA,
                    (VariantReadOperationWithAudit<DataResult<Variant>>) (engine, auditAttributes) -> {
                        StopWatch stopWatch = StopWatch.createStarted();
                        final int limit = options.getInt(QueryOptions.LIMIT, 10);
                        int skip = options.getInt(SKIP, 0);
                        // Make initial batchLimit shorter
                        int batchLimit = Math.min(SAMPLE_BATCH_SIZE_DEFAULT, limit * 3 + skip);
                        int batchSkip = 0;

                        String studyFqn = query.getString(STUDY.key());
                        options.putAll(query);
                        options.put(LIMIT, batchLimit);
                        options.put(SKIP, batchSkip);
                        Variant variantResult = null;

                        List<SampleEntry> sampleEntries = new ArrayList<>(limit);
                        List<FileEntry> fileEntries = new ArrayList<>(limit);
                        LinkedHashMap<String, Integer> fileEntriesPosition = new LinkedHashMap<>(limit);

                        // Get sample data in batches, then check permissions, join and return.
                        boolean moreResults;
                        do {
                            options.put(LIMIT, batchLimit);
                            options.put(SKIP, batchSkip);
                            DataResult<Variant> result = engine.getSampleData(variant, studyFqn, new QueryOptions(options));

                            if (variantResult == null) {
                                variantResult = result.first();
                            }
                            StudyEntry thisStudyEntry = result.first().getStudies().get(0);
                            if (thisStudyEntry.getSamples().isEmpty()) {
                                // End of the story, break loop
                                break;
                            }

                            // Trim results
                            List<String> samplesInResult = thisStudyEntry.getSamples()
                                    .stream()
                                    .map(SampleEntry::getSampleId)
                                    .collect(Collectors.toList());
                            moreResults = samplesInResult.size() == batchLimit;

                            StopWatch checkPermissionsStopWatch = StopWatch.createStarted();
                            String userId = catalogManager.getUserManager().getUserId(token);
                            List<String> validSamples = catalogManager.getSampleManager()
                                    .search(study,
                                            new Query(SampleDBAdaptor.QueryParams.ID.key(), samplesInResult)
                                                    .append(ACL_PARAM, userId + ":" + SampleAclEntry.SamplePermissions.VIEW_VARIANTS),
                                            new QueryOptions(INCLUDE, SampleDBAdaptor.QueryParams.ID.key()), token)
                                    .getResults()
                                    .stream()
                                    .map(Sample::getId)
                                    .sorted()
                                    .collect(Collectors.toList());
                            auditAttributes.put("checkSamplePermissionsTimeMillis",
                                    checkPermissionsStopWatch.getTime(TimeUnit.MILLISECONDS)
                                            + auditAttributes.getInt("checkSamplePermissionsTimeMillis", 0));
                            List<String> samplesToReturn;
                            if (skip > validSamples.size()) {
                                samplesToReturn = Collections.emptyList();
                                skip -= validSamples.size();
                            } else {
                                // limit is always more than sampleEntries.size, as required by the while
                                int remainingLimit = limit - sampleEntries.size();
                                samplesToReturn = validSamples.subList(skip, Math.min(skip + remainingLimit, validSamples.size()));
                                skip = 0;
                            }
                            if (!samplesToReturn.isEmpty()) {
                                for (SampleEntry sample : thisStudyEntry.getSamples()) {
                                    if (samplesToReturn.contains(sample.getSampleId())) {
                                        sampleEntries.add(sample);
                                        if (sample.getFileIndex() != null) {
                                            FileEntry file = thisStudyEntry.getFile(sample.getFileIndex());
                                            if (fileEntriesPosition.putIfAbsent(file.getFileId(), fileEntriesPosition.size()) == null) {
                                                // New file, add to fileEntries
                                                fileEntries.add(file);
                                            }
                                            sample.setFileIndex(fileEntriesPosition.get(file.getFileId()));
                                        }
                                    }
                                }
                            }
                            batchLimit = options.getInt(SAMPLE_BATCH_SIZE, SAMPLE_BATCH_SIZE_DEFAULT);
                            batchSkip += batchLimit;
                        } while (moreResults && sampleEntries.size() < limit);


                        variantResult.getStudies().get(0).setSamples(sampleEntries);
                        variantResult.getStudies().get(0).setFiles(fileEntries);

                        ObjectMap attributes = new ObjectMap();
//                        VariantStats stats = variantResult.getStudies().get(0).getStats(StudyEntry.DEFAULT_COHORT);
//                        if (stats != null) {
//                            List<String> genotypesFilter = new ArrayList<>(options.getAsStringList(GENOTYPE.key()));
//                            if (genotypesFilter.isEmpty()) {
//                                genotypesFilter.add(GenotypeClass.MAIN_ALT.name());
//                            }
//                            int expectedSamplesCount = 0;
//                            List<String> gtsInVariant = new ArrayList<>(stats.getGenotypeCount().keySet());
//                            List<String> genotypesToReturn = GenotypeClass.filter(genotypesFilter, gtsInVariant);
//                            for (String gt : genotypesToReturn) {
//                                expectedSamplesCount += stats.getGenotypeCount().getOrDefault(gt, 0);
//                            }
//                        }
//                        result.getAttributes().put(NUM_SAMPLES.key(), samplesToReturn.size());
//                        result.getAttributes().put(NUM_TOTAL_SAMPLES.key(), validSamples.size());

                        return new DataResult<>(((int) stopWatch.getTime(TimeUnit.MILLISECONDS)),
                                new ArrayList<>(), 1, Collections.singletonList(variantResult), 1, attributes);
                    });
        }
    }

    public StudyMetadata getStudyMetadata(String study, String token)
            throws CatalogException, StorageEngineException {
        String studyFqn = getStudyFqn(study, token);
        Query query = new Query(STUDY.key(), studyFqn)
                .append(INCLUDE_SAMPLE.key(), VariantQueryUtils.NONE)
                .append(INCLUDE_FILE.key(), VariantQueryUtils.NONE);

        return secure(query, new QueryOptions(), token, engine -> {
            VariantStorageMetadataManager metadataManager = engine.getMetadataManager();
            StudyMetadata studyMetadata = metadataManager.getStudyMetadata(studyFqn);
            if (studyMetadata == null) {
                // Sample does not exist in storage!
                throw VariantQueryException.studyNotFound(studyFqn);
            }
            return studyMetadata;
        });
    }

    public SampleMetadata getSampleMetadata(String study, String sample, String token)
            throws CatalogException, StorageEngineException {
        Query query = new Query(STUDY.key(), study)
                .append(SAMPLE.key(), sample)
                .append(INCLUDE_FILE.key(), VariantQueryUtils.NONE);
        return secure(query, new QueryOptions(), token, engine -> {

            VariantStorageMetadataManager metadataManager = engine.getMetadataManager();
            int studyId = metadataManager.getStudyId(study);
            Integer sampleId = metadataManager.getSampleId(studyId, sample);
            if (sampleId == null) {
                // Sample does not exist in storage!
                throw VariantQueryException.sampleNotFound(sample, study);
            } else {
                return metadataManager.getSampleMetadata(studyId, sampleId);
            }
        });
    }

    protected VariantStorageEngine getVariantStorageEngine(Query query, String token) throws CatalogException, StorageEngineException {
        String study = catalogUtils.getAnyStudy(query, token);

        catalogUtils.parseQuery(query, token);
        DataStore dataStore = getDataStore(study, token);
        return getVariantStorageEngine(dataStore);
    }

    protected VariantStorageEngine getVariantStorageEngine(String study, String token) throws StorageEngineException, CatalogException {
        DataStore dataStore = getDataStore(study, token);
        return getVariantStorageEngine(dataStore);
    }

    protected VariantStorageEngine getVariantStorageEngine(String study, ObjectMap params, String token) throws StorageEngineException, CatalogException {
        DataStore dataStore = getDataStore(study, token);
        VariantStorageEngine variantStorageEngine = getVariantStorageEngine(dataStore);
        if (params != null) {
            variantStorageEngine.getOptions().putAll(params);
        }
        return variantStorageEngine;
    }

    protected VariantStorageEngine getVariantStorageEngine(DataStore dataStore) throws StorageEngineException {
        VariantStorageEngine variantStorageEngine = storageEngineFactory
                .getVariantStorageEngine(dataStore.getStorageEngine(), dataStore.getDbName());
        if (dataStore.getConfiguration() != null) {
            variantStorageEngine.getOptions().putAll(dataStore.getConfiguration());
        }
        return variantStorageEngine;
    }

    public boolean isSolrAvailable() {
        SolrManager solrManager = null;
        try {
            solrManager = new SolrManager(
                    storageConfiguration.getSearch().getHosts(),
                    storageConfiguration.getSearch().getMode(),
                    storageConfiguration.getSearch().getTimeout());
            String collectionName = "test_connection";
            if (!solrManager.exists(collectionName)) {
                solrManager.create(collectionName, storageConfiguration.getSearch().getConfigSet());
            }
        } catch (Exception e) {
            logger.warn("Ignore exception checking if Solr is available", e);
            return false;
        } finally {
            if (solrManager != null) {
                try {
                    solrManager.close();
                } catch (IOException e) {
                    logger.warn("Ignore exception closing Solr", e);
                    return false;
                }
            }
        }
        return true;
    }

    public void checkQueryPermissions(Query query, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException {
        VariantStorageEngine variantStorageEngine = getVariantStorageEngine(query, token);
        checkSamplesPermissions(query, queryOptions, variantStorageEngine.getMetadataManager(), token);
    }

    public Set<String> getIndexedSamples(String study, String token) throws CatalogException {
        OpenCGAResult<Cohort> cohortResult = catalogManager
                .getCohortManager()
                .search(study, new Query(CohortDBAdaptor.QueryParams.ID.key(), StudyEntry.DEFAULT_COHORT),
                        new QueryOptions(INCLUDE, Arrays.asList(
                                CohortDBAdaptor.QueryParams.ID.key(),
                                CohortDBAdaptor.QueryParams.SAMPLES.key() + "." + SampleDBAdaptor.QueryParams.ID.key()
                        )), token);
        if (cohortResult.getNumResults() == 0) {
            return Collections.emptySet();
        } else {
            return cohortResult
                    .first()
                    .getSamples()
                    .stream()
                    .map(Sample::getId)
                    .collect(Collectors.toSet());
        }
    }

    public CellBaseUtils getCellBaseUtils(String study, String token) throws StorageEngineException, CatalogException {
        try (VariantStorageEngine storageEngine = getVariantStorageEngine(study, token)) {
            return storageEngine.getCellBaseUtils();
        } catch (IOException e) {
            throw new StorageEngineException("Error closing the VariantStorageEngine", e);
        }
    }

    @Override
    public void close() throws IOException {
        storageEngineFactory.close();
    }

    public boolean synchronizeCatalogStudyFromStorage(String study, List<String> files, String token)
            throws CatalogException, StorageEngineException {
        String studySqn = getStudyFqn(study, token);
        return secureOperation("synchronizeCatalogStudyFromStorage", studySqn, new ObjectMap() ,token, engine -> {
            CatalogStorageMetadataSynchronizer synchronizer =
                    new CatalogStorageMetadataSynchronizer(getCatalogManager(), engine.getMetadataManager());
            if (CollectionUtils.isEmpty(files)) {
                return synchronizer.synchronizeCatalogStudyFromStorage(studySqn, token);
            } else {
                List<File> filesFromCatalog = catalogManager.getFileManager()
                        .get(studySqn, files, FILE_GET_QUERY_OPTIONS, token).getResults();
                return synchronizer.synchronizeCatalogFilesFromStorage(studySqn, filesFromCatalog, token, FILE_GET_QUERY_OPTIONS);
            }
        });
    }

    // Permission related methods

    private interface VariantReadOperation<R> {
        R apply(VariantStorageEngine engine) throws CatalogException, StorageEngineException;
    }

    private interface VariantReadOperationWithAudit<R> extends VariantReadOperation<R> {
        default R apply(VariantStorageEngine engine) throws CatalogException, StorageEngineException {
            return apply(engine, empty());
        }

        R apply(VariantStorageEngine engine, ObjectMap auditAttributes) throws CatalogException, StorageEngineException;
    }

    private interface VariantOperationFunction<R> {
        R apply(VariantStorageEngine engine) throws Exception;
    }

    private <R> R secureOperationByProject(String operationName, String project, ObjectMap params, String token, VariantOperationFunction<R> operation)
            throws CatalogException, StorageEngineException {
        try (VariantStorageEngine variantStorageEngine = getVariantStorageEngine(getDataStoreByProjectId(project, token))) {
            if (params != null) {
                variantStorageEngine.getOptions().putAll(params);
            }
            return secureOperation(operationName, params, token, variantStorageEngine, operation);
        } catch (IOException e) {
            throw new StorageEngineException("Error closing the VariantStorageEngine", e);
        }
    }

    private <R> R secureOperation(String operationName, String study, ObjectMap params, String token, VariantOperationFunction<R> operation)
            throws CatalogException, StorageEngineException {
        try (VariantStorageEngine variantStorageEngine = getVariantStorageEngine(study, params, token)) {
            return secureOperation(operationName, params, token, variantStorageEngine, operation);
        } catch (IOException e) {
            throw new StorageEngineException("Error closing the VariantStorageEngine", e);
        }
    }

    private <R> R secureOperation(String operationName, String projectStr, String study, ObjectMap params, String token, VariantOperationFunction<R> operation)
            throws CatalogException, StorageEngineException {
        List<String> studies = Collections.emptyList();
        if (StringUtils.isNotEmpty(study)) {
            studies = Arrays.asList(study.split(","));
        }
        return secureOperation(operationName, projectStr, studies, params, token, operation);
    }

    private <R> R secureOperation(String operationName, String projectStr, List<String> studies, ObjectMap params, String token,
                                  VariantOperationFunction<R> operation)
            throws CatalogException, StorageEngineException {
        projectStr = getProjectId(projectStr, studies, token);
        return secureOperationByProject(operationName, projectStr, params, token, operation);
    }

    private <R> R secureOperation(String operationName, ObjectMap params, String token,
                                  VariantStorageEngine variantStorageEngine, VariantOperationFunction<R> operation)
            throws CatalogException, StorageEngineException {

        ObjectMap auditAttributes = new ObjectMap()
                .append("dbName", variantStorageEngine.getDBName())
                .append("operationName", operationName);
        R result = null;
        String userId = catalogManager.getUserManager().getUserId(token);
        Exception exception = null;
        StopWatch totalStopWatch = StopWatch.createStarted();

        try {
            result = operation.apply(variantStorageEngine);
            return result;
        } catch (CatalogException | StorageEngineException e) {
            exception = e;
            throw e;
        } catch (Exception e) {
            exception = e;
            throw new StorageEngineException("Error executing operation " + operationName, e);
        } finally {
            if (result instanceof DataResult) {
                auditAttributes.append("dbTime", ((DataResult) result).getTime());
                auditAttributes.append("numResults", ((DataResult) result).getResults().size());
            }
            auditAttributes.append("totalTimeMillis", totalStopWatch.getTime(TimeUnit.MILLISECONDS));
            auditAttributes.append("error", result == null);
            AuditRecord.Status status;
            if (exception != null) {
                auditAttributes.append("errorType", exception.getClass());
                auditAttributes.append("errorMessage", exception.getMessage());
                status = new AuditRecord.Status(AuditRecord.Status.Result.ERROR,
                        new Error(-1, exception.getClass().getName(), exception.getMessage()));
            } else {
                status = new AuditRecord.Status(AuditRecord.Status.Result.SUCCESS);
            }
            logger.debug("dbTime = " + auditAttributes.getInt("dbTime"));
            logger.debug("totalTimeMillis = " + auditAttributes.getInt("totalTimeMillis"));
            catalogManager.getAuditManager().audit(userId, Enums.Action.VARIANT_STORAGE_OPERATION, Enums.Resource.VARIANT,
                    "", "", "", "",
                    params,
                    status,
                    auditAttributes);
        }

    }

    private <R> R secure(Query query, QueryOptions queryOptions, String token, VariantReadOperation<R> supplier)
            throws CatalogException, StorageEngineException {
        VariantStorageEngine variantStorageEngine = getVariantStorageEngine(query, token);
        checkSamplesPermissions(query, queryOptions, variantStorageEngine.getMetadataManager(), token);
        return supplier.apply(variantStorageEngine);
    }

    private <R> R secure(Query query, QueryOptions queryOptions, String token, Enums.Action auditAction,
                         VariantReadOperation<R> supplier)
            throws CatalogException, StorageEngineException, IOException {
        ObjectMap auditAttributes = new ObjectMap()
                .append("query", new Query(query))
                .append("queryOptions", new QueryOptions(queryOptions));
        R result = null;
        String userId = catalogManager.getUserManager().getUserId(token);
        String dbName = null;
        Exception exception = null;
        StopWatch totalStopWatch = StopWatch.createStarted();
        StopWatch storageStopWatch = null;
        try {
            String study = catalogUtils.getAnyStudy(query, token);

            StopWatch stopWatch = StopWatch.createStarted();
            catalogUtils.parseQuery(query, token);
            auditAttributes.append("catalogParseQueryTimeMillis", stopWatch.getTime(TimeUnit.MILLISECONDS));
            DataStore dataStore = getDataStore(study, token);
            VariantStorageEngine variantStorageEngine = getVariantStorageEngine(dataStore);

            stopWatch = StopWatch.createStarted();
            checkSamplesPermissions(query, queryOptions, variantStorageEngine.getMetadataManager(), auditAction, token);
            auditAttributes.append("checkPermissionsTimeMillis", stopWatch.getTime(TimeUnit.MILLISECONDS));

            storageStopWatch = StopWatch.createStarted();
            if (supplier instanceof VariantReadOperationWithAudit) {
                result = ((VariantReadOperationWithAudit<R>) supplier).apply(variantStorageEngine, auditAttributes);
            } else {
                result = supplier.apply(variantStorageEngine);
            }
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            auditAttributes.append("storageTimeMillis", storageStopWatch == null
                    ? -1
                    : storageStopWatch.getTime(TimeUnit.MILLISECONDS));
            if (result instanceof DataResult) {
                auditAttributes.append("dbTime", ((DataResult) result).getTime());
                auditAttributes.append("numResults", ((DataResult) result).getResults().size());
            }
            auditAttributes.append("totalTimeMillis", totalStopWatch.getTime(TimeUnit.MILLISECONDS));
            auditAttributes.append("error", result == null);
            AuditRecord.Status status;
            if (exception != null) {
                auditAttributes.append("errorType", exception.getClass());
                auditAttributes.append("errorMessage", exception.getMessage());
                status = new AuditRecord.Status(AuditRecord.Status.Result.ERROR,
                        new Error(-1, exception.getClass().getName(), exception.getMessage()));
            } else {
                status = new AuditRecord.Status(AuditRecord.Status.Result.SUCCESS);
            }
            logger.debug("catalogParseQueryTimeMillis = " + auditAttributes.getInt("catalogParseQueryTimeMillis"));
            logger.debug("checkPermissionsTimeMillis = " + auditAttributes.getInt("checkPermissionsTimeMillis"));
            logger.debug("storageTimeMillis = " + auditAttributes.getInt("storageTimeMillis"));
            logger.debug("dbTime = " + auditAttributes.getInt("dbTime"));
            logger.debug("totalTimeMillis = " + auditAttributes.getInt("totalTimeMillis"));
            catalogManager.getAuditManager().audit(userId, auditAction, Enums.Resource.VARIANT, "", "", "", "", new ObjectMap(),
                    status, auditAttributes);
        }
    }

    private Map<String, List<String>> checkSamplesPermissions(Query query, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException, IOException {
        String study = catalogUtils.getAnyStudy(query, token);
        DataStore dataStore = getDataStore(study, token);
        VariantStorageEngine variantStorageEngine = getVariantStorageEngine(dataStore);
        return checkSamplesPermissions(query, queryOptions, variantStorageEngine.getMetadataManager(), token);
    }

    // package protected for test visibility
    Map<String, List<String>> checkSamplesPermissions(Query query, QueryOptions queryOptions, VariantStorageMetadataManager mm,
                                                      String token) throws CatalogException {
        return checkSamplesPermissions(query, queryOptions, mm, null, token);
    }

    Map<String, List<String>> checkSamplesPermissions(Query query, QueryOptions queryOptions, VariantStorageMetadataManager mm,
                                                      Enums.Action auditAction, String token)
            throws CatalogException {
        final Map<String, List<String>> samplesMap = new HashMap<>();
        String userId = catalogManager.getUserManager().getUserId(token);
        Set<VariantField> returnedFields = VariantField.getIncludeFields(queryOptions);
        if (auditAction == Enums.Action.FACET) {
            if (!VariantQueryProjectionParser.isIncludeSamplesDefined(query, VariantField.getIncludeFields(null))) {
                // General facet query. Do not check samples.
                returnedFields = Collections.emptySet();
            }
        }

        if (!returnedFields.contains(VariantField.STUDIES_SAMPLES) && !returnedFields.contains(VariantField.STUDIES_FILES)) {
            if (isValidParam(query, STUDY)) {
                ParsedQuery<String> studies = VariantQueryUtils.splitValue(query, STUDY);
                studies.getValues().replaceAll(VariantQueryUtils::removeNegation);
                checkStudyPermissions(studies.getValues(), userId, token);
            } else {
                // Check permissions on each study
                List<String> studies = mm.getStudyNames();
                List<String> validStudies = checkStudyPermissionsAny(studies, userId, token);
                if (validStudies.size() != studies.size()) {
                    query.put(STUDY.key(), validStudies);
                }
            }
            return Collections.emptyMap();
        }

        if (VariantQueryProjectionParser.isIncludeSamplesDefined(query, returnedFields)) {
            Map<String, List<String>> samplesToReturn = VariantQueryUtils.getSamplesMetadata(query, queryOptions, mm);
            checkStudyPermissions(samplesToReturn.keySet(), userId, token);

            for (Map.Entry<String, List<String>> entry : samplesToReturn.entrySet()) {
                String studyId = entry.getKey();
                if (!entry.getValue().isEmpty()) {
//                    DataResult<Sample> samplesQueryResult = catalogManager.getSampleManager().get(studyId, entry.getValue(),
//                            new QueryOptions(INCLUDE, SampleDBAdaptor.QueryParams.ID.key()), token);
                    long numMatches = catalogManager.getSampleManager()
                            .count(studyId, new Query(SampleDBAdaptor.QueryParams.ID.key(), entry.getValue())
                                    .append(ACL_PARAM, userId + ":" + SampleAclEntry.SamplePermissions.VIEW_VARIANTS), token)
                            .getNumMatches();
                    if (numMatches != entry.getValue().size()) {
                        throw new CatalogAuthorizationException("Permission denied. User "
                                + userId + " can't read all the requested samples");
                    }
                    samplesMap.put(studyId, entry.getValue());
                } else {
                    samplesMap.put(studyId, Collections.emptyList());
                }
            }
        } else {
            logger.debug("Missing include samples! Obtaining samples to include from catalog.");
            List<String> includeStudies = VariantQueryProjectionParser.getIncludeStudies(query, queryOptions, mm)
                    .stream()
                    .map(mm::getStudyName)
                    .collect(Collectors.toList());
            if (VariantQueryProjectionParser.isIncludeStudiesDefined(query)) {
                checkStudyPermissions(includeStudies, userId, token);
            } else {
                List<String> validStudies = checkStudyPermissionsAny(includeStudies, userId, token);
                if (validStudies.size() != includeStudies.size()) {
                    query.put(STUDY.key(), validStudies);
                }
                includeStudies = validStudies;
            }

            if (!returnedFields.contains(VariantField.STUDIES_SAMPLES)) {
                for (String returnedStudy : includeStudies) {
                    samplesMap.put(returnedStudy, Collections.emptyList());
                }
            } else {
                List<String> includeSamplesAll = new LinkedList<>();
                for (String study: includeStudies) {
                    study = getStudyFqn(study, token);
                    DBIterator<Sample> iterator = catalogManager.getSampleManager().iterator(
                            study,
                            new Query()
                                    .append(ACL_PARAM, userId + ":" + SampleAclEntry.SamplePermissions.VIEW_VARIANTS),
                            new QueryOptions()
                                    .append(INCLUDE, SampleDBAdaptor.QueryParams.ID.key())
//                                    .append(SORT, "id")
//                                    .append(ORDER, ASCENDING)
                                    .append("lazy", true), token);

                    List<String> includeSamples = new LinkedList<>();
                    int studyId = mm.getStudyId(study);
                    while (iterator.hasNext()) {
                        Sample sample = iterator.next();
                        if (mm.getSampleId(studyId, sample.getId(), true) != null) {
                            includeSamples.add(sample.getId());
                            includeSamplesAll.add(sample.getId());
                        }
                    }
                    samplesMap.put(study, includeSamples);
                }
                if (includeSamplesAll.isEmpty()) {
                    query.append(VariantQueryParam.INCLUDE_SAMPLE.key(), NONE);
                } else {
                    query.append(VariantQueryParam.INCLUDE_SAMPLE.key(), includeSamplesAll);
                }
            }
        }
        return samplesMap;
    }

    private List<String> checkStudyPermissionsAny(Collection<String> studies, String userId, String token) throws CatalogException {
        CatalogException exception = null;
        List<String> validStudies = new ArrayList<>();
        for (String study : studies) {
            try {
                checkStudyPermissions(study, userId, token);
                validStudies.add(study);
            } catch (CatalogException e) {
                exception = e;
            }
        }
        if (!validStudies.isEmpty() || exception == null) {
            return validStudies;
        } else {
            throw exception;
        }
    }

    private void checkStudyPermissions(Collection<String> studies, String userId, String token) throws CatalogException {
        for (String study : studies) {
            checkStudyPermissions(study, userId, token);
        }
    }

    private void checkStudyPermissions(String study, String userId, String token) throws CatalogException {
        long studyUid = catalogManager.getStudyManager().get(study, StudyManager.INCLUDE_STUDY_ID, token).first().getUid();
        CatalogAuthorizationException exception = null;

        // Check VIEW_AGGREGATED_VARIANTS
        try {
            catalogManager.getAuthorizationManager()
                    .checkStudyPermission(studyUid, userId, StudyAclEntry.StudyPermissions.VIEW_AGGREGATED_VARIANTS);
            return;
        } catch (CatalogAuthorizationException e) {
            exception = e;
        }

        // Check VIEW_SAMPLE_VARIANTS
        try {
            catalogManager.getAuthorizationManager()
                    .checkStudyPermission(studyUid, userId, StudyAclEntry.StudyPermissions.VIEW_SAMPLE_VARIANTS);
            return;
        } catch (CatalogAuthorizationException e) {
            // Ignore this exception. Throw exception of missing VIEW_AGGREGATED_VARIANTS
            logger.debug("Ignore exception ", e);
        }

        // Check VIEW_VARIANTS on any sample
        long count = catalogManager.getSampleManager()
                .count(study, new Query(ACL_PARAM, userId + ":" + SampleAclEntry.SamplePermissions.VIEW_VARIANTS), token).getNumMatches();
        if (count != 0) {
            return;
        }

        // Didn't pass any check. Throw exception!
        throw exception;
    }

    // Some aux methods

    private String[] getRegions(Query query) {
        String[] regions;
        String regionStr = query.getString(VariantQueryParam.REGION.key());
        if (!StringUtils.isEmpty(regionStr)) {
            regions = regionStr.split(",");
        } else {
            regions = new String[0];
        }
        return regions;
    }

    public static Query getVariantQuery(Map<String, ?> queryOptions) {
        Query query = new Query();

        for (VariantQueryParam queryParams : VariantQueryParam.values()) {
            if (queryOptions.containsKey(queryParams.key())) {
                query.put(queryParams.key(), queryOptions.get(queryParams.key()));
            }
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.SAMPLE_ANNOTATION.key())) {
            query.put(VariantCatalogQueryUtils.SAMPLE_ANNOTATION.key(), queryOptions.get(VariantCatalogQueryUtils.SAMPLE_ANNOTATION.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.PROJECT.key())) {
            query.put(VariantCatalogQueryUtils.PROJECT.key(), queryOptions.get(VariantCatalogQueryUtils.PROJECT.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.FAMILY.key())) {
            query.put(VariantCatalogQueryUtils.FAMILY.key(), queryOptions.get(VariantCatalogQueryUtils.FAMILY.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.FAMILY_DISORDER.key())) {
            query.put(VariantCatalogQueryUtils.FAMILY_DISORDER.key(), queryOptions.get(VariantCatalogQueryUtils.FAMILY_DISORDER.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.FAMILY_PROBAND.key())) {
            query.put(VariantCatalogQueryUtils.FAMILY_PROBAND.key(), queryOptions.get(VariantCatalogQueryUtils.FAMILY_PROBAND.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.FAMILY_SEGREGATION.key())) {
            query.put(VariantCatalogQueryUtils.FAMILY_SEGREGATION.key(),
                    queryOptions.get(VariantCatalogQueryUtils.FAMILY_SEGREGATION.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.FAMILY_MEMBERS.key())) {
            query.put(VariantCatalogQueryUtils.FAMILY_MEMBERS.key(),
                    queryOptions.get(VariantCatalogQueryUtils.FAMILY_MEMBERS.key()));
        }
        if (queryOptions.containsKey(VariantCatalogQueryUtils.PANEL.key())) {
            query.put(VariantCatalogQueryUtils.PANEL.key(), queryOptions.get(VariantCatalogQueryUtils.PANEL.key()));
        }

        return query;
    }

    @Override
    public void testConnection() throws StorageEngineException {

    }

    // ---------------------//
    //   Facet methods      //
    // ---------------------//

    public DataResult<FacetField> facet(Query query, QueryOptions queryOptions, String token)
            throws CatalogException, StorageEngineException, IOException {
        return secure(query, queryOptions, token, Enums.Action.FACET, engine -> {
            logger.debug("getFacets {}, {}", query, queryOptions);
            DataResult<FacetField> result = engine.facet(query, queryOptions);
            logger.debug("getFacets in {}ms", result.getTime());
            return result;
        });
    }

    public List<BeaconResponse> beacon(String beaconsStr, BeaconResponse.Query beaconQuery, String token)
            throws CatalogException, IOException, StorageEngineException {
        if (beaconsStr.startsWith("[")) {
            beaconsStr = beaconsStr.substring(1);
        }
        if (beaconsStr.endsWith("]")) {
            beaconsStr = beaconsStr.substring(0, beaconsStr.length() - 1);
        }

        List<String> beaconsList = Arrays.asList(beaconsStr.split(","));

        List<BeaconResponse.Beacon> beacons = new ArrayList<>(beaconsList.size());
        for (String studyStr : beaconsList) {
            beacons.add(new BeaconResponse.Beacon(studyStr, null, null, null));
        }

        List<BeaconResponse> responses = new ArrayList<>(beacons.size());

        for (BeaconResponse.Beacon beacon : beacons) {
            Query query = new Query();
            query.put(STUDY.key(), beacon.getId());
            int position1based = beaconQuery.getPosition() + 1;
            query.put(REGION.key(), new Region(beaconQuery.getChromosome(), position1based, position1based));
            query.putIfNotEmpty(REFERENCE.key(), beaconQuery.getReference());
            switch (beaconQuery.getAllele().toUpperCase()) {
                case "D":
                case "DEL":
                    query.put(TYPE.key(), VariantType.DELETION);
                    break;
                case "I":
                case "INS":
                    query.put(TYPE.key(), VariantType.INSERTION);
                    break;
                default:
                    query.put(ALTERNATE.key(), beaconQuery.getAllele());
                    break;
            }

            Long count = count(query, token).first();
            if (count > 1) {
                throw new VariantQueryException("Unexpected beacon count for query " + query + ". Got " + count + " results!");
            }
            BeaconResponse beaconResponse = new BeaconResponse(beacon, beaconQuery, count == 1, null);

            responses.add(beaconResponse);
        }
        return responses;
    }

    private List<String> getStudiesFqn(List<String> studies, String token) throws CatalogException {
        List<String> studiesFqn = null;
        if (studies != null) {
            studiesFqn = new ArrayList<>(studies.size());
            for (String study : studies) {
                studiesFqn.add(getStudyFqn(study, token));
            }
        }
        return studiesFqn;
    }

    private String getStudyFqn(String study, String token) throws CatalogException {
        return catalogManager.getStudyManager().get(study, StudyManager.INCLUDE_STUDY_ID, token).first().getFqn();
    }

    private String getProjectId(String projectStr, String study, String token) throws CatalogException {
        return getProjectId(projectStr, Arrays.asList(StringUtils.split(study, ",")), token);
    }

    private String getProjectId(String projectStr, List<String> studies, String token) throws CatalogException {
        if (CollectionUtils.isEmpty(studies) && StringUtils.isEmpty(projectStr)) {
            List<Project> projects = catalogManager.getProjectManager().get(new Query(), new QueryOptions(), token).getResults();
            if (projects.size() == 1) {
                projectStr = projects.get(0).getFqn();
            } else {
                throw new IllegalArgumentException("Expected either studies or project to annotate");
            }
        }

        if (CollectionUtils.isNotEmpty(studies)) {
            // Ensure all studies are valid. Convert to FQN
            studies = catalogManager.getStudyManager()
                    .get(studies, StudyManager.INCLUDE_STUDY_ID, false, token)
                    .getResults()
                    .stream()
                    .map(Study::getFqn)
                    .collect(Collectors.toList());

            projectStr = catalogManager.getStudyManager().getProjectFqn(studies.get(0));

            if (studies.size() > 1) {
                for (String studyStr : studies) {
                    if (!projectStr.equals(catalogManager.getStudyManager().getProjectFqn(studyStr))) {
                        throw new CatalogException("Can't operate on studies from different projects!");
                    }
                }
            }
        }
        return projectStr;
    }

    public DataStore getDataStore(String study, String token) throws CatalogException {
        return getDataStore(catalogManager, study, File.Bioformat.VARIANT, token);
    }

    public DataStore getDataStoreByProjectId(String project, String token) throws CatalogException {
        return getDataStoreByProjectId(catalogManager, project, File.Bioformat.VARIANT, token);
    }

    public static DataStore getDataStore(CatalogManager catalogManager, String studyStr, File.Bioformat bioformat, String token)
            throws CatalogException {
        Study study = catalogManager.getStudyManager().get(studyStr, new QueryOptions(), token).first();
        return getDataStore(catalogManager, study, bioformat, token);
    }

    private static DataStore getDataStore(CatalogManager catalogManager, Study study, File.Bioformat bioformat, String token)
            throws CatalogException {
        String projectId = catalogManager.getStudyManager().getProjectFqn(study.getFqn());
        return getDataStoreByProjectId(catalogManager, projectId, bioformat, token);
    }

    public static DataStore getDataStoreByProjectId(CatalogManager catalogManager, String projectStr, File.Bioformat bioformat,
                                                    String token)
            throws CatalogException {
        DataStore dataStore = null;
        QueryOptions queryOptions = new QueryOptions(INCLUDE,
                Arrays.asList(ProjectDBAdaptor.QueryParams.ID.key(), ProjectDBAdaptor.QueryParams.INTERNAL_DATASTORES.key()));
        Project project = catalogManager.getProjectManager().get(projectStr, queryOptions, token).first();
        if (project.getInternal() != null && project.getInternal().getDatastores() != null) {
            dataStore = project.getInternal().getDatastores().getDataStore(bioformat);
        }

        if (dataStore == null) { //get default datastore
            //Must use the UserByStudyId instead of the file owner.
            String userId = catalogManager.getProjectManager().getOwner(project.getUid());
            // Replace possible dots at the userId. Usually a special character in almost all databases. See #532
            userId = userId.replace('.', '_');

            String databasePrefix = catalogManager.getConfiguration().getDatabasePrefix();

            String dbName = buildDatabaseName(databasePrefix, userId, project.getId());
            dataStore = new DataStore(StorageEngineFactory.get().getDefaultStorageEngineId(), dbName);
        }
        if (dataStore.getConfiguration() == null) {
            dataStore.setConfiguration(new ObjectMap());
        }

        return dataStore;
    }

    public static String buildDatabaseName(String databasePrefix, String userId, String alias) {
        String prefix;
        if (StringUtils.isNotEmpty(databasePrefix)) {
            prefix = databasePrefix;
            if (!prefix.endsWith("_")) {
                prefix += "_";
            }
        } else {
            prefix = "opencga_";
        }
        // Project alias contains the userId:
        // userId@projectAlias
        int idx = alias.indexOf('@');
        if (idx >= 0) {
            alias = alias.substring(idx + 1);
        }

        return prefix + userId + '_' + alias;
    }

}
