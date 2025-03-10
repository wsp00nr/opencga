"""
WARNING: AUTOGENERATED CODE

    This code was generated by a tool.
    Autogenerated on: 2020-11-19 12:03:49
    
    Manual changes to this file may cause unexpected behavior in your application.
    Manual changes to this file will be overwritten if the code is regenerated.
"""

from pyopencga.rest_clients._parent_rest_clients import _ParentRestClient


class Cohort(_ParentRestClient):
    """
    This class contains methods for the 'Cohorts' webservices
    Client version: 2.0.0
    PATH: /{apiVersion}/cohorts
    """

    def __init__(self, configuration, token=None, login_handler=None, *args, **kwargs):
        super(Cohort, self).__init__(configuration, token, login_handler, *args, **kwargs)

    def update_acl(self, members, action, data=None, **options):
        """
        Update the set of permissions granted for the member.
        PATH: /{apiVersion}/cohorts/acl/{members}/update

        :param dict data: JSON containing the parameters to add ACLs.
            (REQUIRED)
        :param str action: Action to be performed [ADD, SET, REMOVE or RESET].
            (REQUIRED)
        :param str members: Comma separated list of user or group ids.
            (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        """

        options['action'] = action
        return self._post(category='cohorts', resource='update', subcategory='acl', second_query_id=members, data=data, **options)

    def aggregation_stats(self, **options):
        """
        Fetch catalog cohort stats.
        PATH: /{apiVersion}/cohorts/aggregationStats

        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str type: Type.
        :param str creation_year: Creation year.
        :param str creation_month: Creation month (JANUARY, FEBRUARY...).
        :param str creation_day: Creation day.
        :param str creation_day_of_week: Creation day of week (MONDAY,
            TUESDAY...).
        :param str num_samples: Number of samples.
        :param str status: Status.
        :param str release: Release.
        :param str annotation: Annotation filters. Example:
            age>30;gender=FEMALE. For more information, please visit
            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
        :param bool default: Calculate default stats.
        :param str field: List of fields separated by semicolons, e.g.:
            studies;type. For nested fields use >>, e.g.:
            studies>>biotype;type;numSamples[0..10]:1.
        """

        return self._get(category='cohorts', resource='aggregationStats', **options)

    def load_annotation_sets(self, variable_set_id, path, data=None, **options):
        """
        Load annotation sets from a TSV file.
        PATH: /{apiVersion}/cohorts/annotationSets/load

        :param str path: Path where the TSV file is located in OpenCGA or
            where it should be located. (REQUIRED)
        :param str variable_set_id: Variable set ID or name. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param bool parents: Flag indicating whether to create parent
            directories if they don't exist (only when TSV file was not
            previously associated).
        :param str annotation_set_id: Annotation set id. If not provided,
            variableSetId will be used.
        :param dict data: JSON containing the 'content' of the TSV file if
            this has not yet been registered into OpenCGA.
        """

        options['variable_set_id'] = variable_set_id
        options['path'] = path
        return self._post(category='cohorts', resource='load', subcategory='annotationSets', data=data, **options)

    def create(self, data=None, **options):
        """
        Create a cohort.
        PATH: /{apiVersion}/cohorts/create

        :param dict data: JSON containing cohort information. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str variable_set: Deprecated: Use /generate web service and
            filter by annotation.
        :param str variable: Deprecated: Use /generate web service and filter
            by annotation.
        """

        return self._post(category='cohorts', resource='create', data=data, **options)

    def distinct(self, field, **options):
        """
        Cohort distinct method.
        PATH: /{apiVersion}/cohorts/distinct

        :param str field: Field for which to obtain the distinct values.
            (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str type: Cohort type.
        :param str creation_date: Creation date. Format: yyyyMMddHHmmss.
            Examples: >2018, 2017-2018, <201805.
        :param str modification_date: Modification date. Format:
            yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        :param bool deleted: Boolean to retrieve deleted cohorts.
        :param str internal_status: Filter by internal status.
        :param str status: Filter by status.
        :param str annotation: Annotation filters. Example:
            age>30;gender=FEMALE. For more information, please visit
            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
        :param str acl: Filter entries for which a user has the provided
            permissions. Format: acl={user}:{permissions}. Example:
            acl=john:WRITE,WRITE_ANNOTATIONS will return all entries for which
            user john has both WRITE and WRITE_ANNOTATIONS permissions. Only
            study owners or administrators can query by this field. .
        :param str samples: Sample list.
        :param str release: Release value.
        """

        options['field'] = field
        return self._get(category='cohorts', resource='distinct', **options)

    def generate(self, data=None, **options):
        """
        Create a cohort based on a sample query.
        PATH: /{apiVersion}/cohorts/generate

        :param dict data: JSON containing cohort information. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str id: Comma separated list sample IDs or UUIDs up to a
            maximum of 100.
        :param bool somatic: Somatic sample.
        :param str individual_id: Individual ID.
        :param str file_ids: Comma separated list of file IDs.
        :param str creation_date: Creation date. Format: yyyyMMddHHmmss.
            Examples: >2018, 2017-2018, <201805.
        :param str modification_date: Modification date. Format:
            yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        :param str internal_status: Filter by internal status.
        :param str status: Filter by status.
        :param str phenotypes: Comma separated list of phenotype ids or names.
        :param str annotation: Annotation filters. Example:
            age>30;gender=FEMALE. For more information, please visit
            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
        :param str acl: Filter entries for which a user has the provided
            permissions. Format: acl={user}:{permissions}. Example:
            acl=john:WRITE,WRITE_ANNOTATIONS will return all entries for which
            user john has both WRITE and WRITE_ANNOTATIONS permissions. Only
            study owners or administrators can query by this field. .
        :param str release: Release when it was created.
        :param int snapshot: Snapshot value (Latest version of the entry in
            the specified release).
        """

        return self._post(category='cohorts', resource='generate', data=data, **options)

    def search(self, **options):
        """
        Search cohorts.
        PATH: /{apiVersion}/cohorts/search

        :param str include: Fields included in the response, whole JSON path
            must be provided.
        :param str exclude: Fields excluded in the response, whole JSON path
            must be provided.
        :param int limit: Number of results to be returned.
        :param int skip: Number of results to skip.
        :param bool count: Get the total number of results matching the query.
            Deactivated by default.
        :param bool flatten_annotations: Flatten the annotations?.
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str name: DEPRECATED: Name of the cohort.
        :param str type: Cohort type.
        :param str creation_date: Creation date. Format: yyyyMMddHHmmss.
            Examples: >2018, 2017-2018, <201805.
        :param str modification_date: Modification date. Format:
            yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        :param bool deleted: Boolean to retrieve deleted cohorts.
        :param str internal_status: Filter by internal status.
        :param str status: Filter by status.
        :param str annotation: Annotation filters. Example:
            age>30;gender=FEMALE. For more information, please visit
            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
        :param str acl: Filter entries for which a user has the provided
            permissions. Format: acl={user}:{permissions}. Example:
            acl=john:WRITE,WRITE_ANNOTATIONS will return all entries for which
            user john has both WRITE and WRITE_ANNOTATIONS permissions. Only
            study owners or administrators can query by this field. .
        :param str samples: Sample list.
        :param str release: Release value.
        """

        return self._get(category='cohorts', resource='search', **options)

    def acl(self, cohorts, **options):
        """
        Return the acl of the cohort. If member is provided, it will only
            return the acl for the member.
        PATH: /{apiVersion}/cohorts/{cohorts}/acl

        :param str cohorts: Comma separated list of cohort names or IDs up to
            a maximum of 100. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str member: User or group id.
        :param bool silent: Boolean to retrieve all possible entries that are
            queried for, false to raise an exception whenever one of the
            entries looked for cannot be shown for whichever reason.
        """

        return self._get(category='cohorts', resource='acl', query_id=cohorts, **options)

    def delete(self, cohorts, **options):
        """
        Delete cohorts.
        PATH: /{apiVersion}/cohorts/{cohorts}/delete

        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str cohorts: Comma separated list of cohort ids.
        """

        return self._delete(category='cohorts', resource='delete', query_id=cohorts, **options)

    def info(self, cohorts, **options):
        """
        Get cohort information.
        PATH: /{apiVersion}/cohorts/{cohorts}/info

        :param str cohorts: Comma separated list of cohort names or IDs up to
            a maximum of 100. (REQUIRED)
        :param str include: Fields included in the response, whole JSON path
            must be provided.
        :param str exclude: Fields excluded in the response, whole JSON path
            must be provided.
        :param bool flatten_annotations: Flatten the annotations?.
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param bool deleted: Boolean to retrieve deleted cohorts.
        """

        return self._get(category='cohorts', resource='info', query_id=cohorts, **options)

    def update(self, cohorts, data=None, **options):
        """
        Update some cohort attributes.
        PATH: /{apiVersion}/cohorts/{cohorts}/update

        :param str cohorts: Comma separated list of cohort ids. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str samples_action: Action to be performed if the array of
            samples is being updated. Allowed values: ['ADD', 'SET', 'REMOVE']
        :param str annotation_sets_action: Action to be performed if the array
            of annotationSets is being updated. Allowed values: ['ADD', 'SET',
            'REMOVE']
        :param dict data: body.
        """

        return self._post(category='cohorts', resource='update', query_id=cohorts, data=data, **options)

    def update_annotations(self, cohort, annotation_set, data=None, **options):
        """
        Update annotations from an annotationSet.
        PATH: /{apiVersion}/cohorts/{cohort}/annotationSets/{annotationSet}/annotations/update

        :param str cohort: Cohort ID. (REQUIRED)
        :param str study: Study [[user@]project:]study where study and project
            can be either the ID or UUID.
        :param str annotation_set: AnnotationSet ID to be updated.
        :param str action: Action to be performed: ADD to add new annotations;
            REPLACE to replace the value of an already existing annotation; SET
            to set the new list of annotations removing any possible old
            annotations; REMOVE to remove some annotations; RESET to set some
            annotations to the default value configured in the corresponding
            variables of the VariableSet if any. Allowed values: ['ADD', 'SET',
            'REMOVE', 'RESET', 'REPLACE']
        :param dict data: Json containing the map of annotations when the
            action is ADD, SET or REPLACE, a json with only the key 'remove'
            containing the comma separated variables to be removed as a value
            when the action is REMOVE or a json with only the key 'reset'
            containing the comma separated variables that will be set to the
            default value when the action is RESET.
        """

        return self._post(category='cohorts', resource='annotations/update', query_id=cohort, subcategory='annotationSets', second_query_id=annotation_set, data=data, **options)

