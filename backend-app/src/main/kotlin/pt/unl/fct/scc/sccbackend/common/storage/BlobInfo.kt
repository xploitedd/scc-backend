package pt.unl.fct.scc.sccbackend.common.storage

data class BlobInfo(
    val data: ByteArray,
    val contentType: String,
    val hash: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlobInfo

        if (hash != other.hash) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}