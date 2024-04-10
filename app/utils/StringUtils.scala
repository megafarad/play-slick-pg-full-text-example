package utils

object StringUtils {
  def ellipses(original: String, maxLength: Int): String = {
    if (original.length <= maxLength){
      original
    } else {
      original.substring(0, maxLength - 3) + "..."
    }
  }
}
