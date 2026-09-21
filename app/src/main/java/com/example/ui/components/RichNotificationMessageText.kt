package com.example.ui.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * High quality Rich Text Parser and Renderer for Notifications & Message Previews.
 * Supports HTML tags (<b>, <i>, <u>, <s>, <code>, <a href>) and Markdown formatting (**, *, _, ~~, `).
 */
fun parseRichNotificationAnnotatedString(
    rawText: String,
    primaryColor: Color,
    linkColor: Color = primaryColor
): AnnotatedString {
    if (rawText.isBlank()) return AnnotatedString("")

    // Decode HTML entities
    val decoded = rawText
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")

    // Combined regex for HTML formatting and Markdown
    val regex = Regex(
        "(?s)" +
        "(<b>|<strong>|\\*\\*|__)(.*?)(</b>|</strong>|\\*\\*|__)|" +
        "(<i>|<em>|\\*|_)(.*?)(</i>|</em>|\\*|_)|" +
        "(<u>)(.*?)(</u>)|" +
        "(<s>|<strike>|<del>|~~)(.*?)(</s>|</strike>|</del>|~~)|" +
        "(<code>|`)(.*?)(</code>|`)|" +
        "(\\[(.*?)\\]\\((.*?)\\))"
    )

    return buildAnnotatedString {
        var lastIndex = 0
        val matches = regex.findAll(decoded)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > lastIndex) {
                append(decoded.substring(lastIndex, start))
            }

            when {
                // Bold: <b>...</b> or **...** or __...__
                match.groupValues[1].isNotEmpty() -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[2])
                    }
                }
                // Italic: <i>...</i> or *...* or _..._
                match.groupValues[4].isNotEmpty() -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[5])
                    }
                }
                // Underline: <u>...</u>
                match.groupValues[7].isNotEmpty() -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(match.groupValues[8])
                    }
                }
                // Strikethrough: <s>...</s> or ~~...~~
                match.groupValues[9].isNotEmpty() -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(match.groupValues[10])
                    }
                }
                // Code: <code>...</code> or `...`
                match.groupValues[11].isNotEmpty() -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    ) {
                        append(match.groupValues[12])
                    }
                }
                // Link: [title](url)
                match.groupValues[13].isNotEmpty() -> {
                    val linkTitle = match.groupValues[14]
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(linkTitle)
                    }
                }
                else -> {
                    append(match.value)
                }
            }

            lastIndex = end
        }

        if (lastIndex < decoded.length) {
            append(decoded.substring(lastIndex))
        }
    }
}

@Composable
fun RichNotificationMessageText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    isSelectable: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text, primaryColor) {
        parseRichNotificationAnnotatedString(text, primaryColor)
    }

    if (isSelectable) {
        SelectionContainer(modifier = modifier) {
            Text(
                text = annotatedString,
                color = color,
                fontSize = fontSize,
                lineHeight = lineHeight,
                maxLines = maxLines,
                overflow = overflow
            )
        }
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
