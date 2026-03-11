package vinz.android.imagesqueeze

/**
 * Defines the specific reason why compression failed, aiding developer actions.
 */
enum class SqueezeError {
    FILE_NOT_FOUND,
    NOT_READABLE,
    FILE_EMPTY,
    NO_DISK_SPACE,
    DECODE_FAILED,
    OUT_OF_MEMORY,
    COPY_FAILED,
    UNKNOWN
}
