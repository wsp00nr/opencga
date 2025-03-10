package org.opencb.opencga.storage.core.rga;

import org.opencb.opencga.core.models.analysis.knockout.KnockoutVariant;
import org.opencb.opencga.storage.core.exceptions.RgaException;
import org.opencb.opencga.storage.core.variant.query.KeyOpValue;
import org.opencb.opencga.storage.core.variant.query.VariantQueryUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencb.opencga.storage.core.rga.RgaDataModel.*;

class RgaUtils {

    private static final Pattern OPERATION_PATTERN = Pattern.compile("^([^=<>~!]*)(<?<=?|>>?=?|!=?|!?=?~|==?)([^=<>~!]+.*)$");

    static final String SEPARATOR = "__";

    private static final Map<String, String> ENCODE_MAP;
    private static final Map<String, String> DECODE_MAP;

    private static final List<Float> POP_FREQS;
    private static final List<String> POP_FREQ_STUDIES;

    public static final String THOUSAND_GENOMES_STUDY = "1kG_phase3";
    public static final String GNOMAD_GENOMES_STUDY = "GNOMAD_GENOMES";

    public static final String PASS = "PASS";
    public static final String NOT_PASS = "NOT_PASS";

    public static final Set<String> ALL_PARAMS;
    public static final Map<String, Set<String>> PARAM_TYPES;

    static {
        ENCODE_MAP = new HashMap<>();

        // KNOCKOUT TYPE
        ENCODE_MAP.put(KnockoutVariant.KnockoutType.COMP_HET.name(), "CH");
        ENCODE_MAP.put(KnockoutVariant.KnockoutType.HOM_ALT.name(), "HOA");
        ENCODE_MAP.put(KnockoutVariant.KnockoutType.HET_ALT.name(), "HEA");
        ENCODE_MAP.put(KnockoutVariant.KnockoutType.DELETION_OVERLAP.name(), "DO");

        // FILTER
        ENCODE_MAP.put(PASS, "P");
        ENCODE_MAP.put(NOT_PASS, "NP");

        // CONSEQUENCE TYPE
        List<String> consequenceTypeList = Arrays.asList("start_retained_variant", "upstream_variant", "3_prime_UTR_variant",
                "splice_acceptor_variant", "transcript_amplification", "upstream_gene_variant", "RNA_polymerase_promoter",
                "non_coding_transcript_exon_variant", "non_coding_transcript_variant", "inframe_variant", "transcript_ablation",
                "splice_donor_variant", "synonymous_variant", "feature_elongation", "feature_truncation", "miRNA_target_site",
                "exon_variant", "downstream_gene_variant", "stop_retained_variant", "TF_binding_site_variant", "initiator_codon_variant",
                "coding_sequence_variant", "protein_altering_variant", "intergenic_variant", "terminator_codon_variant",
                "frameshift_variant", "DNAseI_hypersensitive_site", "feature_variant", "2KB_downstream_variant", "intron_variant",
                "splice_region_variant", "5_prime_UTR_variant", "SNP", "stop_gained", "regulatory_region_amplification",
                "2KB_upstream_variant", "miRNA", "lincRNA", "start_lost", "SNV", "CpG_island", "downstream_variant",
                "NMD_transcript_variant", "2KB_downstream_gene_variant", "TFBS_amplification", "missense_variant",
                "regulatory_region_ablation", "mature_miRNA_variant", "stop_lost", "structural_variant", "regulatory_region_variant",
                "TFBS_ablation", "copy_number_change", "2KB_upstream_gene_variant", "polypeptide_variation_site", "inframe_deletion",
                "inframe_insertion", "incomplete_terminal_codon_variant");
        for (String consequenceType : consequenceTypeList) {
            ENCODE_MAP.put(consequenceType, String.valueOf(VariantQueryUtils.parseConsequenceType(consequenceType)));
        }

        // POPULATION FREQUENCY
        POP_FREQ_STUDIES = Arrays.asList(THOUSAND_GENOMES_STUDY, GNOMAD_GENOMES_STUDY);
        POP_FREQS = Arrays.asList(0f, 0.0001f, 0.0005f, 0.001f, 0.005f, 0.01f, 0.05f, 1f);
        for (int i = 1; i <= POP_FREQ_STUDIES.size(); i++) {
            String popFreqStudy = POP_FREQ_STUDIES.get(i - 1);
            for (int j = 1; j <= POP_FREQS.size(); j++) {
                Float popFreq = POP_FREQS.get(j - 1);
                ENCODE_MAP.put(popFreqStudy.toUpperCase() + SEPARATOR + popFreq, "P" + i + "-" + j);
            }
        }

        // Generate decodeMap
        DECODE_MAP = new HashMap<>();
        for (Map.Entry<String, String> entry : ENCODE_MAP.entrySet()) {
            DECODE_MAP.put(entry.getValue(), entry.getKey());
        }

        // All params
        ALL_PARAMS = new HashSet<>();
        ALL_PARAMS.add(RgaDataModel.ID);
        ALL_PARAMS.add(RgaDataModel.INDIVIDUAL_ID);
        ALL_PARAMS.add(RgaDataModel.SAMPLE_ID);
        ALL_PARAMS.add(RgaDataModel.SEX);
        ALL_PARAMS.add(RgaDataModel.PHENOTYPES);
        ALL_PARAMS.add(RgaDataModel.DISORDERS);
        ALL_PARAMS.add(RgaDataModel.GENE_ID);
        ALL_PARAMS.add(RgaDataModel.GENE_NAME);
        ALL_PARAMS.add(RgaDataModel.GENE_BIOTYPE);
        ALL_PARAMS.add(RgaDataModel.CHROMOSOME);
        ALL_PARAMS.add(RgaDataModel.STRAND);
        ALL_PARAMS.add(RgaDataModel.START);
        ALL_PARAMS.add(RgaDataModel.END);
        ALL_PARAMS.add(RgaDataModel.TRANSCRIPT_ID);
        ALL_PARAMS.add(RgaDataModel.TRANSCRIPT_BIOTYPE);
        ALL_PARAMS.add(RgaDataModel.VARIANTS);
        ALL_PARAMS.add(RgaDataModel.KNOCKOUT_TYPES);
        ALL_PARAMS.add(RgaDataModel.FILTERS);
        ALL_PARAMS.add(RgaDataModel.CONSEQUENCE_TYPES);
        ALL_PARAMS.add(RgaDataModel.POPULATION_FREQUENCIES);
        ALL_PARAMS.add(RgaDataModel.COMPOUND_FILTERS);
        ALL_PARAMS.add(PHENOTYPE_JSON);
        ALL_PARAMS.add(DISORDER_JSON);
        ALL_PARAMS.add(VARIANT_JSON);

        PARAM_TYPES = new HashMap<>();
        // Variant params
        Set<String> params = new HashSet<>();
        params.add(VARIANTS);
        params.add(KNOCKOUT_TYPES);
        params.add(FILTERS);
        params.add(CONSEQUENCE_TYPES);
        params.add(POPULATION_FREQUENCIES);
        params.add(COMPOUND_FILTERS);
        params.add(PHENOTYPE_JSON);
        params.add(DISORDER_JSON);
        params.add(VARIANT_JSON);
        PARAM_TYPES.put("variants", params);

        // Transcript params
        params = new HashSet<>();
        params.add(TRANSCRIPT_ID);
        params.add(TRANSCRIPT_BIOTYPE);
        params.addAll(PARAM_TYPES.get("variants"));
        PARAM_TYPES.put("transcripts", params);

        // Gene params
        params = new HashSet<>();
        params.add(GENE_ID);
        params.add(GENE_NAME);
        params.add(GENE_BIOTYPE);
        params.add(CHROMOSOME);
        params.add(STRAND);
        params.add(START);
        params.add(END);
        params.addAll(PARAM_TYPES.get("transcripts"));
        PARAM_TYPES.put("genes", params);
    }

    static String getPopulationFrequencyKey(Float popFreq) throws RgaException {
        for (Float freq : POP_FREQS) {
            if (popFreq <= freq) {
                return String.valueOf(freq);
            }
        }
        throw new RgaException("Population frequency must be a value between 0 and 1. Passed '" + popFreq + "'.");
    }

    static String encode(String value) throws RgaException {
        if (ENCODE_MAP.containsKey(value)) {
            return ENCODE_MAP.get(value);
        } else {
            throw new RgaException("Unknown filter value '" + value + "'");
        }
    }

    static String decode(String value) throws RgaException {
        if (DECODE_MAP.containsKey(value)) {
            return DECODE_MAP.get(value);
        } else {
            throw new RgaException("Unknown filter value '" + value + "'");
        }
    }

    /** Calculate the list of population frequency values to look for in the db.
     *
     * @param filters A list containing {study}[<|>|<=|>=]{number}. e.g. 1kG_phase3<0.01";
     * @return the list of population frequency values to look for in the db.
     * @throws RgaException RgaException.
     */
    static List<List<String>> parsePopulationFrequencyQuery(List<String> filters) throws RgaException {
        List<List<String>> result = new ArrayList<>(filters.size());
        for (String filter : filters) {
            KeyOpValue<String, String> keyOpValue = parseKeyOpValue(filter);
            if (keyOpValue.getKey() == null) {
                throw new RgaException("Unexpected operation '" + filter + "'");
            }

            List<String> values = new LinkedList<>();
            float value = Float.parseFloat(keyOpValue.getValue());
            switch (keyOpValue.getOp()) {
                case "<":
                    for (int i = 0; POP_FREQS.get(i) < value; i++) {
                        Float popFreq = POP_FREQS.get(i);
                        values.add(encode(keyOpValue.getKey().toUpperCase() + SEPARATOR + popFreq));
                    }
                    break;
                case "<=":
                    for (int i = 0; POP_FREQS.get(i) <= value; i++) {
                        Float popFreq = POP_FREQS.get(i);
                        values.add(encode(keyOpValue.getKey().toUpperCase() + SEPARATOR + popFreq));
                    }
                    break;
                case ">":
                    for (int i = POP_FREQS.size() - 1; POP_FREQS.get(i) > value; i--) {
                        Float popFreq = POP_FREQS.get(i);
                        values.add(encode(keyOpValue.getKey().toUpperCase() + SEPARATOR + popFreq));
                    }
                    break;
                case ">=":
                    for (int i = POP_FREQS.size() - 1; POP_FREQS.get(i) >= value; i--) {
                        Float popFreq = POP_FREQS.get(i);
                        values.add(encode(keyOpValue.getKey().toUpperCase() + SEPARATOR + popFreq));
                    }
                    break;
                default:
                    throw new RgaException("Unknown operator '" + keyOpValue.getOp() + "'");
            }

            result.add(values);
        }

        return result;
    }

    static List<String> parseFilterQuery(List<String> filters) throws RgaException {
        List<String> result = new ArrayList<>(filters.size());
        for (String filter : filters) {
            result.add(encode(filter));
        }
        return result;
    }

    static List<String> parseKnockoutTypeQuery(List<String> knockoutValues) throws RgaException {
        List<String> result = new ArrayList<>(knockoutValues.size());
        for (String knockoutValue : knockoutValues) {
            result.add(encode(knockoutValue.toUpperCase()));
        }
        return result;
    }

    /**
     * This method parses a typical key-op-value param such as 'ALL<=0.2' in an array ["ALL", "<=", "0.2"].
     * In case of not having a key, first element will be empty
     * In case of not matching with {@link #OPERATION_PATTERN}, key will be null and will use the default operator "="
     *
     * @param value The key-op-value parameter to be parsed
     * @return KeyOpValue
     */
    static KeyOpValue<String, String> parseKeyOpValue(String value) {
        Matcher matcher = OPERATION_PATTERN.matcher(value);

        if (matcher.find()) {
            return new KeyOpValue<>(matcher.group(1).trim(), matcher.group(2).trim(), matcher.group(3).trim());
        } else {
            return new KeyOpValue<>(null, "=", value.trim());
        }
    }



}
