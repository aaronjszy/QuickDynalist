package com.louiskirsch.quickdynalist.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call


interface DynalistService {
    @POST("file/list")
    fun listFiles(@Body request: AuthenticatedRequest): Call<FilesResponse>

    @POST("inbox/add")
    fun addToInbox(@Body request: InboxRequest): Call<InboxItemResponse>

    @POST("doc/read")
    fun readDocument(@Body request: ReadDocumentRequest): Call<DocumentResponse>

    @POST("doc/edit")
    fun addToDocument(@Body request: InsertItemRequest): Call<InsertedItemsResponse>

    @POST("doc/edit")
    fun addToDocument(@Body request: BulkInsertItemRequest): Call<InsertedItemsResponse>

    @POST("doc/edit")
    fun moveItem(@Body request: MoveItemRequest): Call<DynalistResponse>

    @POST("doc/edit")
    fun deleteItem(@Body request: DeleteItemRequest): Call<DynalistResponse>

    @POST("doc/edit")
    fun editItem(@Body request: EditItemRequest): Call<DynalistResponse>

    @POST("doc/edit")
    fun editItems(@Body request: BulkEditItemRequest): Call<DynalistResponse>

    @POST("doc/check_for_updates")
    fun checkForUpdates(@Body request: VersionsRequest): Call<VersionResponse>

    @POST("pref/get")
    fun getPreference(@Body request: PreferenceRequest): Call<PreferenceResponse>

    @POST("pref/set")
    fun setPreference(@Body request: SetPreferenceRequest): Call<DynalistResponse>

    @POST("upload")
    fun uploadFile(@Body request: UploadFileRequest): Call<UploadResponse>
}

class AuthenticatedRequest(val token: String)
class InboxRequest(val content: String, val note: String, val token: String)
class ReadDocumentRequest(val file_id: String, val token: String)
class VersionsRequest(val file_ids: Array<String>, val token: String)

class PreferenceRequest(val key: String, val token: String)
open class SetPreferenceRequest(val key: String, val value: String, val token: String)
class SetInboxRequest(fileId: String, nodeId: String, token: String)
    : SetPreferenceRequest("inbox_location", "$fileId/$nodeId", token)

class UploadFileRequest(val filename: String, val content_type: String,
                        val data: String, val token: String)

class InsertItemRequest(val file_id: String, parent_id: String, content: String, note: String,
                        checkbox: Boolean, val token: String, color: Int = 0, index: Int = -1) {

    class InsertSpec(val parent_id: String, val content: String, val note: String,
                     val checkbox: Boolean, val color: Int = 0, val index: Int = -1) {
        val action: String = "insert"
    }

    val changes = arrayOf(InsertSpec(parent_id, content, note, checkbox, color, index))
}

class BulkInsertItemRequest(val file_id: String, val token: String,
                            val changes: Array<InsertItemRequest.InsertSpec>)

class EditItemRequest(val file_id: String, node_id: String,
                      content: String, note: String, checked: Boolean, checkbox: Boolean,
                      heading: Int, color: Int, val token: String) {

    class EditSpec(val node_id: String, val content: String, val note: String,
                   val checked: Boolean, val checkbox: Boolean, val heading: Int, val color: Int) {
        val action: String = "edit"
    }

    val changes = arrayOf(EditSpec(node_id, content, note, checked, checkbox, heading, color))
}
class BulkEditItemRequest(val file_id: String, val token: String,
                          val changes: Array<EditItemRequest.EditSpec>)

class MoveItemRequest(val file_id: String, parent_id: String, node_id: String,
                      index: Int, val token: String) {

    class MoveSpec(val parent_id: String, val node_id: String, val index: Int) {
        val action: String = "move"
    }

    val changes = arrayOf(MoveSpec(parent_id, node_id, index))
}

class DeleteItemRequest(val file_id: String, node_id: String, val token: String) {

    class DeleteSpec(val node_id: String) {
        val action: String = "delete"
    }

    val changes = arrayOf(DeleteSpec(node_id))
}

open class DynalistResponse {
    val _code: String? = null
    val _msg: String? = null

    val isRateLimitExceeded: Boolean
        get() = _code == "TooManyRequests"

    val isInvalidToken: Boolean
        get() = _code == "InvalidToken"

    val isInboxNotConfigured: Boolean
        get() = _code == "NoInbox"

    val isRequestUnfulfillable: Boolean
        get() = _code in listOf("Unauthorized", "NotFound", "NodeNotFound")

    val isOK: Boolean
        get() = _code == "Ok"

    val errorDesc: String
        get() = "Code: $_code; Message: $_msg"
}

class VersionResponse: DynalistResponse() {
    val versions: Map<String, Long>? = null
}

class PreferenceResponse: DynalistResponse() {
    val key: String? = null
    val value: String? = null

    val parsedItemValue: Pair<String, String>? get() {
        if (value == null || value.isBlank()) return null
        val s = value.split('/')
        if (s.size == 2) {
            return Pair(s[0], s[1])
        }
        return Pair(value, "root")
    }
}

class InboxItemResponse: DynalistResponse() {
    val node_id: String? = null
}

class InsertedItemsResponse: DynalistResponse() {
    val new_node_ids: List<String>? = null
}

class File {
    val id: String? = null
    val title: String? = null
    val type: String? = null
    val permission: Int? = null
    val children: List<String>? = null

    val isEditable: Boolean
        get() = permission!! >= 2

    val isDocument: Boolean
        get() = type == "document"

    val isFolder: Boolean
        get() = type == "folder"
}

class FilesResponse: DynalistResponse() {
    val files: List<File>? = null
    val root_file_id: String? = null
}

class Node(val id: String, val content: String, val note: String, val checked: Boolean,
           val checkbox: Boolean, val color: Int, val heading: Int, val created: Long,
           val modified: Long, val collapsed: Boolean,
           val parent: String?, val children: List<String>?)

class DocumentResponse: DynalistResponse() {
    val nodes: List<Node>? = null
}

class UploadResponse: DynalistResponse() {
    val url: String? = null

    val isProNeeded: Boolean
        get() = _code == "NeedPro"

    val isQuotaExceeded: Boolean
        get() = _code == "QuotaExceeded"
}