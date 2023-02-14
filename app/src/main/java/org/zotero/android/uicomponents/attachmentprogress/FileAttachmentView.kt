package org.zotero.android.uicomponents.attachmentprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.database.objects.Attachment
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun FileAttachmentView(
    modifier: Modifier = Modifier,
    state: State,
    style: Style,
) {
    Box(modifier = modifier) {
        set(state = state, style = style)
    }
}

@Composable
private fun BoxScope.set(state: State, style: Style) {
    val type = contentType(state = state, style = style)
    if (type == null) {
        return
    }
    set(contentType = type, style = style)
}

@Composable
private fun BoxScope.set(
    contentType: ContentType,
    style: Style,
) {
    when (contentType) {
        is ContentType.progress -> {
            set(progress = contentType.progress, showsStop = (style != Style.lookup))
            setMainImage(asset = null)
            setBadge(asset = null, style = style)
        }
        is ContentType.image -> {
            set(progress = null, showsStop = false)
            setMainImage(asset = contentType.asset)
            setBadge(asset = null, style = style)
        }
        is ContentType.imageWithBadge -> {
            set(progress = null, showsStop = false)
            setMainImage(asset = contentType.main)
            setBadge(asset = contentType.badge, style = style)
        }
    }
}

@Composable
fun BoxScope.setMainImage(asset: Int?) {
    if (asset != null) {
        Image(
            modifier = Modifier.fillMaxSize(fraction = 0.9f),
            painter = painterResource(id = asset),
            contentDescription = null,
        )
    }
}

@Composable
fun BoxScope.setBadge(asset: Int?, style: Style) {
    if (asset != null) {
        Image(
            modifier = Modifier
                .fillMaxSize(0.4f)
                .align(Alignment.BottomEnd),
            painter = painterResource(id = asset),
            contentDescription = null,
        )
    }
}

@Composable
private fun BoxScope.set(progress: Int?, showsStop: Boolean) {
    val isHidden = progress == null
    if (showsStop) {
        if (!isHidden) {
            val color = CustomTheme.colors.zoteroBlueWithDarkMode
            Canvas(
                Modifier
                    .size(8.dp)
                    .align(Alignment.Center)
            ) {
                drawRect(
                    color = color
                )
            }
        }
    }
    if (!isHidden) {
        val progressFloat = if (progress == null) 0f else progress / 100f
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = CustomTheme.colors.zoteroBlueWithDarkMode,
            backgroundColor = CustomPalette.CoolGray,
            strokeWidth = 1.dp,
            progress = progressFloat
        )
    }
}

private fun contentType(state: State, style: Style): ContentType? {
    return when (state) {
        is State.progress -> {
            return ContentType.progress(state.progress)
        }
        is State.ready -> {
            return when (val type = state.attachmentKind) {
                is Attachment.Kind.file -> {
                    when (type.location) {
                        Attachment.FileLocation.local -> {
                            ContentType.image(
                                asset = mainAsset(
                                    attachmentType = type,
                                    style = style
                                )
                            )
                        }
                        Attachment.FileLocation.remoteMissing -> {
                            ContentType.imageWithBadge(
                                main = mainAsset(
                                    attachmentType = type,
                                    style = style
                                ),
                                badge = badge(BadgeType.missing, style)
                            )
                        }
                        Attachment.FileLocation.remote, Attachment.FileLocation.localAndChangedRemotely -> {
                            ContentType.imageWithBadge(
                                main = mainAsset(
                                    attachmentType = type,
                                    style = style
                                ),
                                badge = badge(BadgeType.download, style)
                            )
                        }
                    }
                }
                is Attachment.Kind.url -> {
                    ContentType.image(asset = mainAsset(type, style))
                }
            }
        }
        is State.failed -> {
            val type = state.attachmendKind
            ContentType.imageWithBadge(
                main = mainAsset(
                    attachmentType = type,
                    style = style
                ),
                badge = badge(BadgeType.failed, style)
            )
        }
    }
}

private fun mainAsset(attachmentType: Attachment.Kind, style: Style): Int {
    return when (attachmentType) {
        is Attachment.Kind.url -> {
            return when (style) {
                Style.detail, Style.shareExtension, Style.lookup -> {
                    Drawables.attachment_detail_linked_url
                }
                Style.list -> {
                    Drawables.list_link
                }
            }
        }
        is Attachment.Kind.file -> {
            val contentType = attachmentType.contentType
            val linkType = attachmentType.linkType
            return when {
                linkType == Attachment.FileLinkType.linkedFile -> {
                    return when (style) {
                        Style.detail, Style.shareExtension, Style.lookup -> {
                            return when (contentType) {
                                "application/pdf" -> Drawables.attachment_detail_linked_pdf
                                else -> Drawables.attachment_detail_linked_document
                            }
                        }
                        Style.list -> {
                            Drawables.list_link
                        }
                    }
                }
                linkType == Attachment.FileLinkType.importedUrl && contentType == "text/html" -> {
                    return when (style) {
                        Style.detail, Style.shareExtension, Style.lookup -> {
                            Drawables.attachment_detail_webpage_snapshot
                        }
                        Style.list -> {
                            Drawables.attachment_list_web_page_snapshot
                        }
                    }
                }
                linkType == Attachment.FileLinkType.importedFile ||
                        linkType == Attachment.FileLinkType.embeddedImage -> {
                    when (contentType) {
                        "image/png", "image/jpeg", "image/gif" -> {
                            when (style) {
                                Style.detail, Style.shareExtension, Style.lookup -> {
                                    Drawables.attachment_detail_image
                                }
                                Style.list -> {
                                    Drawables.attachment_list_image
                                }
                            }
                        }

                        "application/pdf" -> {
                            when (style) {
                                Style.detail, Style.shareExtension, Style.lookup -> {
                                    Drawables.attachment_detail_pdf
                                }
                                Style.list -> {
                                    Drawables.attachment_list_pdf
                                }
                            }
                        }
                        else -> {
                            when (style) {
                                Style.detail, Style.shareExtension, Style.lookup -> {
                                    Drawables.attachment_detail_document
                                }
                                Style.list -> {
                                    Drawables.attachment_list_document
                                }
                            }
                        }
                    }
                }
                else -> {
                    throw java.lang.RuntimeException("Should not be reachable")
                }
            }
        }
    }
}

private fun badge(type: BadgeType, style: Style): Int {
    return when (type) {
        BadgeType.download -> {
            when (style) {
                Style.detail, Style.shareExtension, Style.lookup -> {
                    Drawables.attachment_detail_download
                }
                Style.list -> {
                    Drawables.badge_list_download
                }
            }
        }
        BadgeType.failed -> {
            when (style) {
                Style.detail, Style.lookup -> {
                    Drawables.attachment_detail_download_failed
                }
                Style.shareExtension -> {
                    Drawables.badge_shareext_failed
                }
                Style.list -> {
                    Drawables.badge_list_failed
                }
            }
        }
        BadgeType.missing -> {
            when (style) {
                Style.detail, Style.shareExtension, Style.lookup -> {
                    Drawables.attachment_detail_missing
                }
                Style.list -> {
                    Drawables.badge_list_missing
                }
            }
        }
    }
}

enum class Style {
    list,
    detail,
    shareExtension,
    lookup,
}

sealed class State {
    data class ready(val attachmentKind: Attachment.Kind) : State()
    data class progress(val progress: Int) : State()
    data class failed(val attachmendKind: Attachment.Kind, val error: Throwable) : State()

    companion object {
        fun stateFrom(type: Attachment.Kind, progress: Int?, error: Throwable?): State {
            if (progress != null) {
                return State.progress(progress)
            }
            if (error != null) {
                return State.failed(type, error)
            }
            return State.ready(type)
        }
    }
}

private sealed class ContentType {
    data class progress(val progress: Int) : ContentType()
    data class image(val asset: Int) : ContentType()
    data class imageWithBadge(val main: Int, val badge: Int) : ContentType()
}

private enum class BadgeType {
    failed, missing, download
}