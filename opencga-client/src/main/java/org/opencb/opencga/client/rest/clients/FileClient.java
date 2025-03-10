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

package org.opencb.opencga.client.rest.clients;

import java.io.DataInputStream;
import org.opencb.commons.datastore.core.FacetField;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.client.exceptions.ClientException;
import org.opencb.opencga.client.rest.AbstractParentClient;
import org.opencb.opencga.core.models.common.TsvAnnotationParams;
import org.opencb.opencga.core.models.file.File;
import org.opencb.opencga.core.models.file.FileAclUpdateParams;
import org.opencb.opencga.core.models.file.FileContent;
import org.opencb.opencga.core.models.file.FileCreateParams;
import org.opencb.opencga.core.models.file.FileFetch;
import org.opencb.opencga.core.models.file.FileLinkParams;
import org.opencb.opencga.core.models.file.FileLinkToolParams;
import org.opencb.opencga.core.models.file.FileTree;
import org.opencb.opencga.core.models.file.FileUpdateParams;
import org.opencb.opencga.core.models.file.PostLinkToolParams;
import org.opencb.opencga.core.models.job.Job;
import org.opencb.opencga.core.response.RestResponse;


/*
* WARNING: AUTOGENERATED CODE
*
* This code was generated by a tool.
* Autogenerated on: 2020-11-19 12:00:56
*
* Manual changes to this file may cause unexpected behavior in your application.
* Manual changes to this file will be overwritten if the code is regenerated.
*/


/**
 * This class contains methods for the File webservices.
 *    Client version: 2.0.0
 *    PATH: files
 */
public class FileClient extends AbstractParentClient {

    public FileClient(String token, ClientConfiguration configuration) {
        super(token, configuration);
    }

    /**
     * Update the set of permissions granted for the member.
     * @param members Comma separated list of user or group ids.
     * @param action Action to be performed [ADD, SET, REMOVE or RESET].
     * @param data JSON containing the parameters to add ACLs.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<ObjectMap> updateAcl(String members, String action, FileAclUpdateParams data, ObjectMap params)
            throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.putIfNotNull("action", action);
        params.put("body", data);
        return execute("files", null, "acl", members, "update", params, POST, ObjectMap.class);
    }

    /**
     * Fetch catalog file stats.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       name: Name.
     *       type: Type.
     *       format: Format.
     *       bioformat: Bioformat.
     *       creationYear: Creation year.
     *       creationMonth: Creation month (JANUARY, FEBRUARY...).
     *       creationDay: Creation day.
     *       creationDayOfWeek: Creation day of week (MONDAY, TUESDAY...).
     *       status: Status.
     *       release: Release.
     *       external: External.
     *       size: Size.
     *       software: Software.
     *       experiment: Experiment.
     *       numSamples: Number of samples.
     *       numRelatedFiles: Number of related files.
     *       annotation: Annotation filters. Example: age>30;gender=FEMALE. For more information, please visit
     *            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
     *       default: Calculate default stats.
     *       field: List of fields separated by semicolons, e.g.: studies;type. For nested fields use >>, e.g.:
     *            studies>>biotype;type;numSamples[0..10]:1.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FacetField> aggregationStats(ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", null, null, null, "aggregationStats", params, GET, FacetField.class);
    }

    /**
     * Load annotation sets from a TSV file.
     * @param variableSetId Variable set ID or name.
     * @param path Path where the TSV file is located in OpenCGA or where it should be located.
     * @param data JSON containing the 'content' of the TSV file if this has not yet been registered into OpenCGA.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       parents: Flag indicating whether to create parent directories if they don't exist (only when TSV file was not previously
     *            associated).
     *       annotationSetId: Annotation set id. If not provided, variableSetId will be used.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> loadAnnotationSets(String variableSetId, String path, TsvAnnotationParams data, ObjectMap params)
            throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.putIfNotNull("variableSetId", variableSetId);
        params.putIfNotNull("path", path);
        params.put("body", data);
        return execute("files", null, "annotationSets", null, "load", params, POST, Job.class);
    }

    /**
     * List of accepted file bioformats.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File.Bioformat> bioformats() throws ClientException {
        ObjectMap params = new ObjectMap();
        return execute("files", null, null, null, "bioformats", params, GET, File.Bioformat.class);
    }

    /**
     * Create file or folder.
     * @param data File parameters.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> create(FileCreateParams data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", null, null, null, "create", params, POST, File.class);
    }

    /**
     * File distinct method.
     * @param field Field for which to obtain the distinct values.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       name: Comma separated list of file names.
     *       path: Comma separated list of paths.
     *       type: File type, either FILE or DIRECTORY.
     *       bioformat: Comma separated Bioformat values. For existing Bioformats see files/bioformats.
     *       format: Comma separated Format values. For existing Formats see files/formats.
     *       status: Filter by status.
     *       internalStatus: Filter by internal status.
     *       directory: Directory under which we want to look for files or folders.
     *       creationDate: Creation date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
     *       modificationDate: Modification date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
     *       description: Description.
     *       tags: Tags.
     *       size: File size.
     *       sampleIds: Comma separated list sample IDs or UUIDs up to a maximum of 100.
     *       jobId: Job ID that created the file(s) or folder(s).
     *       annotation: Annotation filters. Example: age>30;gender=FEMALE. For more information, please visit
     *            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
     *       acl: Filter entries for which a user has the provided permissions. Format: acl={user}:{permissions}. Example:
     *            acl=john:WRITE,WRITE_ANNOTATIONS will return all entries for which user john has both WRITE and WRITE_ANNOTATIONS
     *            permissions. Only study owners or administrators can query by this field. .
     *       attributes: Text attributes (Format: sex=male,age>20 ...).
     *       release: Release when it was created.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<ObjectMap> distinct(String field, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.putIfNotNull("field", field);
        return execute("files", null, null, null, "distinct", params, GET, ObjectMap.class);
    }

    /**
     * Download an external file to catalog and register it.
     * @param data Fetch parameters.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> fetch(FileFetch data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", null, null, null, "fetch", params, POST, Job.class);
    }

    /**
     * List of accepted file formats.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File.Format> formats() throws ClientException {
        ObjectMap params = new ObjectMap();
        return execute("files", null, null, null, "formats", params, GET, File.Format.class);
    }

    /**
     * Link an external file into catalog.
     * @param data File parameters.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       parents: Create the parent directories if they do not exist.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> link(FileLinkParams data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", null, null, null, "link", params, POST, File.class);
    }

    /**
     * Link an external file into catalog asynchronously.
     * @param data File parameters.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       jobId: Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
     *       jobDependsOn: Comma separated list of existing job IDs the job will depend on.
     *       jobDescription: Job description.
     *       jobTags: Job tags.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> runLink(FileLinkToolParams data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", null, "link", null, "run", params, POST, Job.class);
    }

    /**
     * Associate non-registered samples for files with high volumes of samples.
     * @param data File parameters.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       jobId: Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
     *       jobDependsOn: Comma separated list of existing job IDs the job will depend on.
     *       jobDescription: Job description.
     *       jobTags: Job tags.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> runPostlink(PostLinkToolParams data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", null, "postlink", null, "run", params, POST, Job.class);
    }

    /**
     * File search method.
     * @param params Map containing any of the following optional parameters.
     *       include: Fields included in the response, whole JSON path must be provided.
     *       exclude: Fields excluded in the response, whole JSON path must be provided.
     *       limit: Number of results to be returned.
     *       skip: Number of results to skip.
     *       count: Get the total number of results matching the query. Deactivated by default.
     *       flattenAnnotations: Boolean indicating to flatten the annotations.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       name: Comma separated list of file names.
     *       path: Comma separated list of paths.
     *       type: File type, either FILE or DIRECTORY.
     *       bioformat: Comma separated Bioformat values. For existing Bioformats see files/bioformats.
     *       format: Comma separated Format values. For existing Formats see files/formats.
     *       status: Filter by status.
     *       internalStatus: Filter by internal status.
     *       directory: Directory under which we want to look for files or folders.
     *       creationDate: Creation date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
     *       modificationDate: Modification date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
     *       description: Description.
     *       tags: Tags.
     *       size: File size.
     *       sampleIds: Comma separated list sample IDs or UUIDs up to a maximum of 100.
     *       jobId: Job ID that created the file(s) or folder(s).
     *       annotation: Annotation filters. Example: age>30;gender=FEMALE. For more information, please visit
     *            http://docs.opencb.org/display/opencga/AnnotationSets+1.4.0.
     *       acl: Filter entries for which a user has the provided permissions. Format: acl={user}:{permissions}. Example:
     *            acl=john:WRITE,WRITE_ANNOTATIONS will return all entries for which user john has both WRITE and WRITE_ANNOTATIONS
     *            permissions. Only study owners or administrators can query by this field. .
     *       deleted: Boolean to retrieve deleted entries.
     *       attributes: Text attributes (Format: sex=male,age>20 ...).
     *       release: Release when it was created.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> search(ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", null, null, null, "search", params, GET, File.class);
    }

    /**
     * Resource to upload a file by chunks.
     * @param params Map containing any of the following optional parameters.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> upload(ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", null, null, null, "upload", params, POST, File.class);
    }

    /**
     * Return the acl defined for the file or folder. If member is provided, it will only return the acl for the member.
     * @param files Comma separated list of file IDs or names up to a maximum of 100.
     * @param params Map containing any of the following optional parameters.
     *       study: Comma separated list of Studies [[user@]project:]study where study and project can be either the ID or UUID up to a
     *            maximum of 100.
     *       member: User or group id.
     *       silent: Boolean to retrieve all possible entries that are queried for, false to raise an exception whenever one of the entries
     *            looked for cannot be shown for whichever reason.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<ObjectMap> acl(String files, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", files, null, null, "acl", params, GET, ObjectMap.class);
    }

    /**
     * Delete existing files and folders.
     * @param files Comma separated list of file ids, names or paths.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       files: Comma separated list of file ids, names or paths.
     *       skipTrash: Skip trash and delete the files/folders from disk directly (CANNOT BE RECOVERED).
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> delete(String files, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", files, null, null, "delete", params, DELETE, Job.class);
    }

    /**
     * File info.
     * @param files Comma separated list of file IDs or names up to a maximum of 100.
     * @param params Map containing any of the following optional parameters.
     *       include: Fields included in the response, whole JSON path must be provided.
     *       exclude: Fields excluded in the response, whole JSON path must be provided.
     *       flattenAnnotations: Flatten the annotations?.
     *       files: Comma separated list of file IDs or names up to a maximum of 100.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       deleted: Boolean to retrieve deleted files.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> info(String files, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", files, null, null, "info", params, GET, File.class);
    }

    /**
     * Unlink linked files and folders.
     * @param files Comma separated list of file ids, names or paths.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       files: Comma separated list of file ids, names or paths.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<Job> unlink(String files, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", files, null, null, "unlink", params, DELETE, Job.class);
    }

    /**
     * Update some file attributes.
     * @param files Comma separated list of file ids, names or paths. Paths must be separated by : instead of /.
     * @param data Parameters to modify.
     * @param params Map containing any of the following optional parameters.
     *       files: Comma separated list of file ids, names or paths. Paths must be separated by : instead of /.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       sampleIdsAction: Action to be performed if the array of samples is being updated.
     *       annotationSetsAction: Action to be performed if the array of annotationSets is being updated.
     *       relatedFilesAction: Action to be performed if the array of relatedFiles is being updated.
     *       tagsAction: Action to be performed if the array of tags is being updated.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> update(String files, FileUpdateParams data, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", files, null, null, "update", params, POST, File.class);
    }

    /**
     * Update annotations from an annotationSet.
     * @param file File id, name or path. Paths must be separated by : instead of /.
     * @param annotationSet AnnotationSet ID to be updated.
     * @param data Json containing the map of annotations when the action is ADD, SET or REPLACE, a json with only the key 'remove'
     *     containing the comma separated variables to be removed as a value when the action is REMOVE or a json with only the key 'reset'
     *     containing the comma separated variables that will be set to the default value when the action is RESET.
     * @param params Map containing any of the following optional parameters.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       annotationSet: AnnotationSet ID to be updated.
     *       action: Action to be performed: ADD to add new annotations; REPLACE to replace the value of an already existing annotation;
     *            SET to set the new list of annotations removing any possible old annotations; REMOVE to remove some annotations; RESET to
     *            set some annotations to the default value configured in the corresponding variables of the VariableSet if any.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> updateAnnotations(String file, String annotationSet, ObjectMap data, ObjectMap params)
            throws ClientException {
        params = params != null ? params : new ObjectMap();
        params.put("body", data);
        return execute("files", file, "annotationSets", annotationSet, "annotations/update", params, POST, File.class);
    }

    /**
     * Download file.
     * @param file File id, name or path. Paths must be separated by : instead of /.
     * @param params Map containing any of the following optional parameters.
     *       file: File id, name or path. Paths must be separated by : instead of /.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<DataInputStream> download(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "download", params, GET, DataInputStream.class);
    }

    /**
     * Filter lines of the file containing the pattern.
     * @param file File uuid, id, or name.
     * @param params Map containing any of the following optional parameters.
     *       file: File uuid, id, or name.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       pattern: String pattern.
     *       ignoreCase: Flag to perform a case insensitive search.
     *       maxCount: Stop reading a file after 'n' matching lines. 0 means no limit.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FileContent> grep(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "grep", params, GET, FileContent.class);
    }

    /**
     * Show the first lines of a file (up to a limit).
     * @param file File uuid, id, or name.
     * @param params Map containing any of the following optional parameters.
     *       file: File uuid, id, or name.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       offset: Starting byte from which the file will be read.
     *       lines: Maximum number of lines to be returned.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FileContent> head(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "head", params, GET, FileContent.class);
    }

    /**
     * Obtain the base64 content of an image.
     * @param file File ID.
     * @param params Map containing any of the following optional parameters.
     *       file: File ID.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FileContent> image(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "image", params, GET, FileContent.class);
    }

    /**
     * Refresh metadata from the selected file or folder. Return updated files.
     * @param file File id, name or path. Paths must be separated by : instead of /.
     * @param params Map containing any of the following optional parameters.
     *       file: File id, name or path. Paths must be separated by : instead of /.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> refresh(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "refresh", params, GET, File.class);
    }

    /**
     * Show the last lines of a file (up to a limit).
     * @param file File uuid, id, or name.
     * @param params Map containing any of the following optional parameters.
     *       file: File uuid, id, or name.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       lines: Maximum number of lines to be returned.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FileContent> tail(String file, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", file, null, null, "tail", params, GET, FileContent.class);
    }

    /**
     * List all the files inside the folder.
     * @param folder Folder ID, name or path.
     * @param params Map containing any of the following optional parameters.
     *       include: Fields included in the response, whole JSON path must be provided.
     *       exclude: Fields excluded in the response, whole JSON path must be provided.
     *       limit: Number of results to be returned.
     *       skip: Number of results to skip.
     *       count: Get the total number of results matching the query. Deactivated by default.
     *       folder: Folder ID, name or path.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<File> list(String folder, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", folder, null, null, "list", params, GET, File.class);
    }

    /**
     * Obtain a tree view of the files and folders within a folder.
     * @param folder Folder id or name. Paths must be separated by : instead of /.
     * @param params Map containing any of the following optional parameters.
     *       include: Fields included in the response, whole JSON path must be provided.
     *       exclude: Fields excluded in the response, whole JSON path must be provided.
     *       folder: Folder id or name. Paths must be separated by : instead of /.
     *       study: Study [[user@]project:]study where study and project can be either the ID or UUID.
     *       maxDepth: Maximum depth to get files from.
     * @return a RestResponse object.
     * @throws ClientException ClientException if there is any server error.
     */
    public RestResponse<FileTree> tree(String folder, ObjectMap params) throws ClientException {
        params = params != null ? params : new ObjectMap();
        return execute("files", folder, null, null, "tree", params, GET, FileTree.class);
    }
}
