package net.perfectdreams.harmony.web

import web.dom.Element

// These are from Kotlin, but changed to work with web.dom

/** Returns true if the element has the given CSS class style in its 'class' attribute */
fun Element.hasClass(cssClass: String): Boolean = className.matches("""(^|.*\s+)$cssClass($|\s+.*)""".toRegex())

/**
 * Adds CSS class to element. Has no effect if all specified classes are already in class attribute of the element
 *
 * @return true if at least one class has been added
 */
fun Element.addClass(vararg cssClasses: String): Boolean {
    val missingClasses = cssClasses.filterNot { hasClass(it) }
    if (missingClasses.isNotEmpty()) {
        val presentClasses = className.trim()
        className = buildString {
            append(presentClasses)
            if (!presentClasses.isEmpty()) {
                append(" ")
            }
            missingClasses.joinTo(this, " ")
        }
        return true
    }

    return false
}

/**
 * Removes all [cssClasses] from element. Has no effect if all specified classes are missing in class attribute of the element
 *
 * @return true if at least one class has been removed
 */
fun Element.removeClass(vararg cssClasses: String): Boolean {
    if (cssClasses.any { hasClass(it) }) {
        val toBeRemoved = cssClasses.toSet()
        className = className.trim().split("\\s+".toRegex()).filter { it !in toBeRemoved }.joinToString(" ")
        return true
    }

    return false
}
