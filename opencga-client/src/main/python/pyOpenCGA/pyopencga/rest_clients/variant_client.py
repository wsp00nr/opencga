from pyopencga.rest_clients._parent_rest_clients import _ParentRestClient


class Variant(_ParentRestClient):
    """
    This class contains methods for the 'Analysis - Variant' webservices
    Client version: 2.0.0
    PATH: /{apiVersion}/analysis/variant
    """

    def __init__(self, configuration, token=None, login_handler=None, *args, **kwargs):
        _category = 'analysis/variant'
        super(Variant, self).__init__(configuration, _category, token, login_handler, *args, **kwargs)

    def query(self, **options):
        """
        Filter and fetch variants from indexed VCF files in the variant storage
        PATH: /{apiVersion}/analysis/variant/query

        :param str include: Fields included in the response, whole JSON path must be provided
        :param str exclude: Fields excluded in the response, whole JSON path must be provided
        :param int limit: Number of results to be returned
        :param int skip: Number of results to skip
        :param bool count: Get the total number of results matching the query. Deactivated by default.
        :param bool skip_count: Do not count total number of results
        :param bool sort: Sort the results
        :param bool summary: Fast fetch of main variant parameters
        :param bool approximate_count: Get an approximate count, instead of an exact total count. Reduces execution time
        :param int approximate_count_sampling_size: Sampling size to get the approximate count. Larger values increase accuracy but also increase execution time
        :param str id: List of IDs, these can be rs IDs (dbSNP) or variants in the format chrom:start:ref:alt, e.g. rs116600158,19:7177679:C:T
        :param str region: List of regions, these can be just a single chromosome name or regions in the format chr:start-end, e.g.: 2,3:100000-200000
        :param str type: List of types, accepted values are SNV, MNV, INDEL, SV, CNV, INSERTION, DELETION, e.g. SNV,INDEL
        :param str reference: Reference allele
        :param str alternate: Main alternate allele
        :param str project: Project [user@]project where project can be either the ID or the alias
        :param str study: Filter variants from the given studies, these can be either the numeric ID or the alias with the format user@project:study
        :param str file: Filter variants from the files specified. This will set includeFile parameter when not provided
        :param str filter: Specify the FILTER for any of the files. If 'file' filter is provided, will match the file and the filter. e.g.: PASS,LowGQX
        :param str qual: Specify the QUAL for any of the files. If 'file' filter is provided, will match the file and the qual. e.g.: >123.4
        :param str info: Filter by INFO attributes from file. [{file}:]{key}{op}{value}[,;]* . If no file is specified, will use all files from 'file' filter. e.g. AN>200 or file_1.vcf:AN>200;file_2.vcf:AN<10 . Many INFO fields can be combined. e.g. file_1.vcf:AN>200;DB=true;file_2.vcf:AN<10
        :param str sample: Filter variants where the samples contain the variant (HET or HOM_ALT). Accepts AND (;) and OR (,) operators. This will automatically set 'includeSample' parameter when not provided
        :param str genotype: Samples with a specific genotype: {samp_1}:{gt_1}(,{gt_n})*(;{samp_n}:{gt_1}(,{gt_n})*)* e.g. HG0097:0/0;HG0098:0/1,1/1. Unphased genotypes (e.g. 0/1, 1/1) will also include phased genotypes (e.g. 0|1, 1|0, 1|1), but not vice versa. When filtering by multi-allelic genotypes, any secondary allele will match, regardless of its position e.g. 1/2 will match with genotypes 1/2, 1/3, 1/4, .... Genotype aliases accepted: HOM_REF, HOM_ALT, HET, HET_REF, HET_ALT and MISS  e.g. HG0097:HOM_REF;HG0098:HET_REF,HOM_ALT. This will automatically set 'includeSample' parameter when not provided
        :param str format: Filter by any FORMAT field from samples. [{sample}:]{key}{op}{value}[,;]* . If no sample is specified, will use all samples from 'sample' or 'genotype' filter. e.g. DP>200 or HG0097:DP>200,HG0098:DP<10 . Many FORMAT fields can be combined. e.g. HG0097:DP>200;GT=1/1,0/1,HG0098:DP<10
        :param str sample_annotation: Selects some samples using metadata information from Catalog. e.g. age>20;phenotype=hpo:123,hpo:456;name=smith
        :param bool sample_metadata: Return the samples metadata group by study. Sample names will appear in the same order as their corresponding genotypes.
        :param str unknown_genotype: Returned genotype for unknown genotypes. Common values: [0/0, 0|0, ./.]
        :param int sample_limit: Limit the number of samples to be included in the result
        :param int sample_skip: Skip some samples from the result. Useful for sample pagination.
        :param str cohort: Select variants with calculated stats for the selected cohorts
        :param str cohort_stats_ref: Reference Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_alt: Alternate Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_maf: Minor Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_mgf: Minor Genotype Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str missing_alleles: Number of missing alleles: [{study:}]{cohort}[<|>|<=|>=]{number}
        :param str missing_genotypes: Number of missing genotypes: [{study:}]{cohort}[<|>|<=|>=]{number}
        :param str score: Filter by variant score: [{study:}]{score}[<|>|<=|>=]{number}
        :param str family: Filter variants where any of the samples from the given family contains the variant (HET or HOM_ALT)
        :param str family_disorder: Specify the disorder to use for the family segregation
        :param str family_segregation: Filter by mode of inheritance from a given family. Accepted values: [ monoallelic, monoallelicIncompletePenetrance, biallelic, biallelicIncompletePenetrance, XlinkedBiallelic, XlinkedMonoallelic, Ylinked, MendelianError, DeNovo, CompoundHeterozygous ]
        :param str family_members: Sub set of the members of a given family
        :param str family_proband: Specify the proband child to use for the family segregation
        :param str include_study: List of studies to include in the result. Accepts 'all' and 'none'.
        :param str include_file: List of files to be returned. Accepts 'all' and 'none'.
        :param str include_sample: List of samples to be included in the result. Accepts 'all' and 'none'.
        :param str include_format: List of FORMAT names from Samples Data to include in the output. e.g: DP,AD. Accepts 'all' and 'none'.
        :param str include_genotype: Include genotypes, apart of other formats defined with includeFormat
        :param bool annotation_exists: Return only annotated variants
        :param str gene: List of genes, most gene IDs are accepted (HGNC, Ensembl gene, ...). This is an alias to 'xref' parameter
        :param str ct: List of SO consequence types, e.g. missense_variant,stop_lost or SO:0001583,SO:0001578
        :param str xref: List of any external reference, these can be genes, proteins or variants. Accepted IDs include HGNC, Ensembl genes, dbSNP, ClinVar, HPO, Cosmic, ...
        :param str biotype: List of biotypes, e.g. protein_coding
        :param str protein_substitution: Protein substitution scores include SIFT and PolyPhen. You can query using the score {protein_score}[<|>|<=|>=]{number} or the description {protein_score}[~=|=]{description} e.g. polyphen>0.1,sift=tolerant
        :param str conservation: Filter by conservation score: {conservation_score}[<|>|<=|>=]{number} e.g. phastCons>0.5,phylop<0.1,gerp>0.1
        :param str population_frequency_alt: Alternate Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str population_frequency_ref: Reference Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str population_frequency_maf: Population minor allele frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str transcript_flag: List of transcript annotation flags. e.g. CCDS, basic, cds_end_NF, mRNA_end_NF, cds_start_NF, mRNA_start_NF, seleno
        :param str gene_trait_id: List of gene trait association id. e.g. 'umls:C0007222' , 'OMIM:269600'
        :param str go: List of GO (Gene Ontology) terms. e.g. 'GO:0002020'
        :param str expression: List of tissues of interest. e.g. 'lung'
        :param str protein_keyword: List of Uniprot protein variant annotation keywords
        :param str drug: List of drug names
        :param str functional_score: Functional score: {functional_score}[<|>|<=|>=]{number} e.g. cadd_scaled>5.2 , cadd_raw<=0.3
        :param str clinical_significance: Clinical significance: benign, likely_benign, likely_pathogenic, pathogenic
        :param str custom_annotation: Custom annotation: {key}[<|>|<=|>=]{number} or {key}[~=|=]{text}
        :param str panel: Filter by genes from the given disease panel
        :param str trait: List of traits, based on ClinVar, HPO, COSMIC, i.e.: IDs, histologies, descriptions,...
        :param str group_by: Group variants by: [ct, gene, ensemblGene]
        :param bool histogram: Calculate histogram. Requires one region.
        :param int interval: Histogram interval size
        :param str rank: Ranks different entities with the most number of variants. Rank by: [ct, gene, ensemblGene]
        """

        return self._get('query', **options)

    def metadata_annotation(self, **options):
        """
        Read variant annotations metadata from any saved versions
        PATH: /{apiVersion}/analysis/variant/annotation/metadata

        :param str annotation_id: Annotation identifier
        :param str project: Project [user@]project where project can be either the ID or the alias
        """

        return self._get('annotation/metadata', **options)

    def run_stats(self, data=None, **options):
        """
        Compute variant stats for any cohort and any set of variants. Optionally, index the result in the variant storage database.
        PATH: /{apiVersion}/analysis/variant/stats/run

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Variant stats params
        """

        return self._post('stats/run', data=data, **options)

    def export_stats(self, data=None, **options):
        """
        Export calculated variant stats and frequencies
        PATH: /{apiVersion}/analysis/variant/stats/export

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Variant stats export params
        """

        return self._post('stats/export', data=data, **options)

    def genotypes_family(self, mode_of_inheritance, **options):
        """
        Calculate the possible genotypes for the members of a family
        PATH: /{apiVersion}/analysis/variant/family/genotypes

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str family: Family id
        :param str clinical_analysis: Clinical analysis id
        :param str mode_of_inheritance: Mode of inheritance
        :param str penetrance: Penetrance
        :param str disorder: Disorder id
        """
        options['mode_of_inheritance'] = mode_of_inheritance
        return self._get('family/genotypes', **options)

    def export(self, data=None, **options):
        """
        Filter and export variants from the variant storage to a file
        PATH: /{apiVersion}/analysis/variant/export

        :param str include: Fields included in the response, whole JSON path must be provided
        :param str exclude: Fields excluded in the response, whole JSON path must be provided
        :param int limit: Number of results to be returned
        :param int skip: Number of results to skip
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Variant export params
        """

        return self._post('export', data=data, **options)

    def metadata(self, **options):
        """
        
        PATH: /{apiVersion}/analysis/variant/metadata

        :param str project: Project [user@]project where project can be either the ID or the alias
        :param str study: Filter variants from the given studies, these can be either the numeric ID or the alias with the format user@project:study
        :param str file: Filter variants from the files specified. This will set includeFile parameter when not provided
        :param str sample: Filter variants where the samples contain the variant (HET or HOM_ALT). Accepts AND (;) and OR (,) operators. This will automatically set 'includeSample' parameter when not provided
        :param str include_study: List of studies to include in the result. Accepts 'all' and 'none'.
        :param str include_file: List of files to be returned. Accepts 'all' and 'none'.
        :param str include_sample: List of samples to be included in the result. Accepts 'all' and 'none'.
        :param str include: Fields included in the response, whole JSON path must be provided
        :param str exclude: Fields excluded in the response, whole JSON path must be provided
        """

        return self._get('metadata', **options)

    def aggregation_stats(self, **options):
        """
        Calculate and fetch aggregation stats
        PATH: /{apiVersion}/analysis/variant/aggregationStats

        :param str region: List of regions, these can be just a single chromosome name or regions in the format chr:start-end, e.g.: 2,3:100000-200000
        :param str type: List of types, accepted values are SNV, MNV, INDEL, SV, CNV, INSERTION, DELETION, e.g. SNV,INDEL
        :param str project: Project [user@]project where project can be either the ID or the alias
        :param str study: Filter variants from the given studies, these can be either the numeric ID or the alias with the format user@project:study
        :param str file: Filter variants from the files specified. This will set includeFile parameter when not provided
        :param str filter: Specify the FILTER for any of the files. If 'file' filter is provided, will match the file and the filter. e.g.: PASS,LowGQX
        :param str sample: Filter variants where the samples contain the variant (HET or HOM_ALT). Accepts AND (;) and OR (,) operators. This will automatically set 'includeSample' parameter when not provided
        :param str genotype: Samples with a specific genotype: {samp_1}:{gt_1}(,{gt_n})*(;{samp_n}:{gt_1}(,{gt_n})*)* e.g. HG0097:0/0;HG0098:0/1,1/1. Unphased genotypes (e.g. 0/1, 1/1) will also include phased genotypes (e.g. 0|1, 1|0, 1|1), but not vice versa. When filtering by multi-allelic genotypes, any secondary allele will match, regardless of its position e.g. 1/2 will match with genotypes 1/2, 1/3, 1/4, .... Genotype aliases accepted: HOM_REF, HOM_ALT, HET, HET_REF, HET_ALT and MISS  e.g. HG0097:HOM_REF;HG0098:HET_REF,HOM_ALT. This will automatically set 'includeSample' parameter when not provided
        :param str sample_annotation: Selects some samples using metadata information from Catalog. e.g. age>20;phenotype=hpo:123,hpo:456;name=smith
        :param str cohort: Select variants with calculated stats for the selected cohorts
        :param str cohort_stats_ref: Reference Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_alt: Alternate Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_maf: Minor Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str cohort_stats_mgf: Minor Genotype Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4
        :param str missing_alleles: Number of missing alleles: [{study:}]{cohort}[<|>|<=|>=]{number}
        :param str missing_genotypes: Number of missing genotypes: [{study:}]{cohort}[<|>|<=|>=]{number}
        :param str score: Filter by variant score: [{study:}]{score}[<|>|<=|>=]{number}
        :param str family: Filter variants where any of the samples from the given family contains the variant (HET or HOM_ALT)
        :param str family_disorder: Specify the disorder to use for the family segregation
        :param str family_segregation: Filter by mode of inheritance from a given family. Accepted values: [ monoallelic, monoallelicIncompletePenetrance, biallelic, biallelicIncompletePenetrance, XlinkedBiallelic, XlinkedMonoallelic, Ylinked, MendelianError, DeNovo, CompoundHeterozygous ]
        :param str family_members: Sub set of the members of a given family
        :param str family_proband: Specify the proband child to use for the family segregation
        :param bool annotation_exists: Return only annotated variants
        :param str gene: List of genes, most gene IDs are accepted (HGNC, Ensembl gene, ...). This is an alias to 'xref' parameter
        :param str ct: List of SO consequence types, e.g. missense_variant,stop_lost or SO:0001583,SO:0001578
        :param str xref: List of any external reference, these can be genes, proteins or variants. Accepted IDs include HGNC, Ensembl genes, dbSNP, ClinVar, HPO, Cosmic, ...
        :param str biotype: List of biotypes, e.g. protein_coding
        :param str protein_substitution: Protein substitution scores include SIFT and PolyPhen. You can query using the score {protein_score}[<|>|<=|>=]{number} or the description {protein_score}[~=|=]{description} e.g. polyphen>0.1,sift=tolerant
        :param str conservation: Filter by conservation score: {conservation_score}[<|>|<=|>=]{number} e.g. phastCons>0.5,phylop<0.1,gerp>0.1
        :param str population_frequency_alt: Alternate Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str population_frequency_ref: Reference Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str population_frequency_maf: Population minor allele frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01
        :param str transcript_flag: List of transcript annotation flags. e.g. CCDS, basic, cds_end_NF, mRNA_end_NF, cds_start_NF, mRNA_start_NF, seleno
        :param str gene_trait_id: List of gene trait association id. e.g. 'umls:C0007222' , 'OMIM:269600'
        :param str go: List of GO (Gene Ontology) terms. e.g. 'GO:0002020'
        :param str expression: List of tissues of interest. e.g. 'lung'
        :param str protein_keyword: List of Uniprot protein variant annotation keywords
        :param str drug: List of drug names
        :param str functional_score: Functional score: {functional_score}[<|>|<=|>=]{number} e.g. cadd_scaled>5.2 , cadd_raw<=0.3
        :param str clinical_significance: Clinical significance: benign, likely_benign, likely_pathogenic, pathogenic
        :param str custom_annotation: Custom annotation: {key}[<|>|<=|>=]{number} or {key}[~=|=]{text}
        :param str trait: List of traits, based on ClinVar, HPO, COSMIC, i.e.: IDs, histologies, descriptions,...
        :param str fields: List of facet fields separated by semicolons, e.g.: studies;type. For nested faceted fields use >>, e.g.: chromosome>>type;percentile(gerp)
        """

        return self._get('aggregationStats', **options)

    def index(self, data=None, **options):
        """
        Index variant files into the variant storage
        PATH: /{apiVersion}/analysis/variant/index

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Variant index params
        """

        return self._post('index', data=data, **options)

    def delete_file(self, **options):
        """
        Remove variant files from the variant storage
        PATH: /{apiVersion}/analysis/variant/file/delete

        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str file: Files to remove
        :param bool resume: Resume a previously failed indexation
        """

        return self._delete('file/delete', **options)

    def run_sample(self, data=None, **options):
        """
        Get samples given a set of variants
        PATH: /{apiVersion}/analysis/variant/sample/run

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Sample variant filter params
        """

        return self._post('sample/run', data=data, **options)

    def query_sample(self, **options):
        """
        Get sample data of a given variant
        PATH: /{apiVersion}/analysis/variant/sample/query

        :param int limit: Number of results to be returned
        :param int skip: Number of results to skip
        :param str variant: Variant
        :param str study: Study where all the samples belong to
        :param str genotype: Genotypes that the sample must have to be selected
        """

        return self._get('sample/query', **options)

    def run_sample_stats(self, data=None, **options):
        """
        Compute sample variant stats for the selected list of samples.
        PATH: /{apiVersion}/analysis/variant/sample/stats/run

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Sample variant stats params
        """

        return self._post('sample/stats/run', data=data, **options)

    def info_sample_stats(self, **options):
        """
        Read sample variant stats from list of samples.
        PATH: /{apiVersion}/analysis/variant/sample/stats/info

        :param str study: Study where all the samples belong to
        :param str sample: Comma separated list sample IDs or UUIDs up to a maximum of 100
        """

        return self._get('sample/stats/info', **options)

    def delete_sample_stats(self, **options):
        """
        Delete sample variant stats from a sample.
        PATH: /{apiVersion}/analysis/variant/sample/stats/delete

        :param str study: study
        :param str sample: Sample
        """

        return self._delete('sample/stats/delete', **options)

    def run_cohort_stats(self, data=None, **options):
        """
        Compute cohort variant stats for the selected list of samples.
        PATH: /{apiVersion}/analysis/variant/cohort/stats/run

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Cohort variant stats params
        """

        return self._post('cohort/stats/run', data=data, **options)

    def info_cohort_stats(self, **options):
        """
        Read cohort variant stats from list of cohorts.
        PATH: /{apiVersion}/analysis/variant/cohort/stats/info

        :param str study: study
        :param str cohort: Comma separated list of cohort names or ids up to a maximum of 100
        """

        return self._get('cohort/stats/info', **options)

    def delete_cohort_stats(self, **options):
        """
        Delete cohort variant stats from a cohort.
        PATH: /{apiVersion}/analysis/variant/cohort/stats/delete

        :param str study: study
        :param str cohort: Cohort id or name
        """

        return self._delete('cohort/stats/delete', **options)

    def run_gwas(self, data=None, **options):
        """
        Run a Genome Wide Association Study between two cohorts.
        PATH: /{apiVersion}/analysis/variant/gwas/run

        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Gwas analysis params
        """

        return self._post('gwas/run', data=data, **options)

    def run_plink(self, data=None, **options):
        """
        Plink is a whole genome association analysis toolset, designed to perform a range of basic, large-scale analyses.
        PATH: /{apiVersion}/analysis/variant/plink/run

        :param str study: study
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: Plink params
        """

        return self._post('plink/run', data=data, **options)

    def run_rvtests(self, data=None, **options):
        """
        Rvtests is a flexible software package for genetic association studies
        PATH: /{apiVersion}/analysis/variant/rvtests/run

        :param str study: study
        :param str job_name: Job name
        :param str job _i_d or _u_u_i_d: Job Description
        :param str job_tags: Job tags
        :param dict data: rvtest params
        """

        return self._post('rvtests/run', data=data, **options)

    def query_annotation(self, **options):
        """
        Query variant annotations from any saved versions
        PATH: /{apiVersion}/analysis/variant/annotation/query

        :param str id: List of IDs, these can be rs IDs (dbSNP) or variants in the format chrom:start:ref:alt, e.g. rs116600158,19:7177679:C:T
        :param str region: List of regions, these can be just a single chromosome name or regions in the format chr:start-end, e.g.: 2,3:100000-200000
        :param str include: Fields included in the response, whole JSON path must be provided
        :param str exclude: Fields excluded in the response, whole JSON path must be provided
        :param int limit: Number of results to be returned
        :param int skip: Number of results to skip
        :param str annotation_id: Annotation identifier
        """

        return self._get('annotation/query', **options)
