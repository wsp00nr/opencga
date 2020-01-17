from pyopencga.rest_clients._parent_rest_clients import _ParentRestClient


class VariantOperations(_ParentRestClient):
    """
    This class contains methods for the 'Operations - Variant Storage' webservices
    Client version: 2.0.0
    PATH: /{apiVersion}/operation
    """

    def __init__(self, configuration, token=None, login_handler=None, *args, **kwargs):
        _category = 'operation'
        super(VariantOperations, self).__init__(configuration, _category, token, login_handler, *args, **kwargs)

    def index_family_genotype(self, data=None, **options):
        """
        Build the family index.
        PATH: /{apiVersion}/operation/variant/family/genotype/index

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant family index params.
        """

        return self._post('index', subcategory='variant/family/genotype', data=data, **options)

    def aggregate_variant_family(self, data=None, **options):
        """
        Find variants where not all the samples are present, and fill the empty values.
        PATH: /{apiVersion}/operation/variant/family/aggregate

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant aggregate family params.
        """

        return self._post('aggregate', subcategory='variant/family', data=data, **options)

    def aggregate_variant(self, data=None, **options):
        """
        Find variants where not all the samples are present, and fill the empty values, excluding HOM-REF (0/0) values.
        PATH: /{apiVersion}/operation/variant/aggregate

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant aggregate params.
        """

        return self._post('aggregate', subcategory='variant', data=data, **options)

    def secondary_index_variant(self, data=None, **options):
        """
        Creates a secondary index using a search engine. If samples are provided, sample data will be added to the secondary index.
        PATH: /{apiVersion}/operation/variant/secondaryIndex

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant secondary index params.
        """

        return self._post('secondaryIndex', subcategory='variant', data=data, **options)

    def delete_variant_secondary_index(self, **options):
        """
        Remove a secondary index from the search engine for a specific set of samples.
        PATH: /{apiVersion}/operation/variant/secondaryIndex/delete

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str samples: Samples to remove. Needs to provide all the samples in the secondary index.
        """

        return self._delete('delete', subcategory='variant/secondaryIndex', **options)

    def delete_variant_annotation(self, **options):
        """
        Deletes a saved copy of variant annotation.
        PATH: /{apiVersion}/operation/variant/annotation/delete

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param str annotation_id: Annotation identifier.
        """

        return self._delete('delete', subcategory='variant/annotation', **options)

    def save_variant_annotation(self, data=None, **options):
        """
        Save a copy of the current variant annotation at the database.
        PATH: /{apiVersion}/operation/variant/annotation/save

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param dict data: Variant annotation save params.
        """

        return self._post('save', subcategory='variant/annotation', data=data, **options)

    def index_variant_score(self, data=None, **options):
        """
        Index a variant score in the database.
        PATH: /{apiVersion}/operation/variant/score/index

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant score index params. scoreName: Unique name of the score within the study. cohort1: Cohort used to compute the score. Use the cohort 'ALL' if all samples from the study where used to compute the score. cohort2: Second cohort used to compute the score, typically to compare against the first cohort. If only one cohort was used to compute the score, leave empty. inputColumns: Indicate which columns to load from the input file. Provide the column position (starting in 0) for the column with the score with 'SCORE=n'. Optionally, the PValue column with 'PVALUE=n'. The, to indicate the variant associated with the score, provide either the columns ['CHROM', 'POS', 'REF', 'ALT'], or the column 'VAR' containing a variant representation with format 'chr:start:ref:alt'. e.g. 'CHROM=0,POS=1,REF=3,ALT=4,SCORE=5,PVALUE=6' or 'VAR=0,SCORE=1,PVALUE=2'. resume: Resume a previously failed indexation.
        """

        return self._post('index', subcategory='variant/score', data=data, **options)

    def delete_variant_score(self, **options):
        """
        Remove a variant score in the database.
        PATH: /{apiVersion}/operation/variant/score/delete

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param str name: Unique name of the score within the study.
        :param bool resume: Resume a previously failed remove.
        :param bool force: Force remove of partially indexed scores.
        """

        return self._delete('delete', subcategory='variant/score', **options)

    def index_sample_genotype(self, data=None, **options):
        """
        Build and annotate the sample index.
        PATH: /{apiVersion}/operation/variant/sample/genotype/index

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant sample index params.
        """

        return self._post('index', subcategory='variant/sample/genotype', data=data, **options)

    def index_variant_annotation(self, data=None, **options):
        """
        Create and load variant annotations into the database.
        PATH: /{apiVersion}/operation/variant/annotation/index

        :param str job_name: Job name.
        :param str job _i_d or _u_u_i_d: Job Description.
        :param str job_tags: Job tags.
        :param str project: Project [user@]project where project can be either the ID or the alias.
        :param str study: Study [[user@]project:]study where study and project can be either the ID or UUID.
        :param dict data: Variant annotation index params.
        """

        return self._post('index', subcategory='variant/annotation', data=data, **options)

