package com.insomdroid.nomina.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insomdroid.nomina.data.DrawerFolder
import com.insomdroid.nomina.model.AppItem
import com.insomdroid.nomina.ui.LauncherIcons
import com.insomdroid.nomina.ui.component.AppIcon
import com.insomdroid.nomina.ui.expressive.ExpressivePalette
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily

internal const val FOLDER_MANAGER_FOLDER_ROW_TAG = "folder-manager-folder"
internal const val FOLDER_MANAGER_APP_ROW_TAG = "folder-manager-app"
internal const val FOLDER_MANAGER_SEARCH_TAG = "folder-manager-search"
internal const val FOLDER_MANAGER_NAME_TAG = "folder-manager-name"

/**
 * Settings → App drawer → Folders. A folder page keeps its existing membership rules, rename,
 * delete confirmation and Back behavior; only its presentation uses the Settings control system.
 */
@Composable
internal fun DrawerFoldersManager(
    folders: List<DrawerFolder>?,
    apps: List<AppItem>,
    hiddenKeys: Set<String>,
    onCreate: () -> Unit,
    onRename: (id: Long, name: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onSetMember: (id: Long, key: String, member: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalExpressivePalette.current
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val open = openId?.let { id -> folders?.firstOrNull { it.id == id } }
    LaunchedEffect(openId, folders) {
        if (openId != null && folders != null && open == null) openId = null
    }
    BackHandler {
        if (openId != null) openId = null else onBack()
    }
    Surface(modifier = modifier, color = p.bg) {
        if (folders == null) {
            // The repository is still loading; retain the restored screen until it is ready.
        } else if (open != null) {
            val otherMembers = remember(folders, open.id) {
                folders.filter { it.id != open.id }.flatMapTo(HashSet()) { it.appKeys }
            }
            FolderPage(
                folder = open,
                apps = apps,
                hiddenKeys = hiddenKeys,
                otherMembers = otherMembers,
                onRename = { onRename(open.id, it) },
                onDelete = {
                    onDelete(open.id)
                    openId = null
                },
                onSetMember = { key, member -> onSetMember(open.id, key, member) },
            )
        } else {
            FolderList(folders = folders, apps = apps, onOpen = { openId = it }, onCreate = onCreate)
        }
    }
}

@Composable
private fun FolderList(
    folders: List<DrawerFolder>,
    apps: List<AppItem>,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val p = LocalExpressivePalette.current
    val installed = remember(apps) { apps.mapTo(HashSet()) { it.key } }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            text = stringResource(R.string.settings_drawer_folders),
            color = p.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        if (folders.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_drawer_folders_empty),
                color = p.dim,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = folders, key = { it.id }) { folder ->
                val count = folder.appKeys.count { it in installed }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(folder.id) }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag(FOLDER_MANAGER_FOLDER_ROW_TAG),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = folder.name, color = p.text, fontSize = 16.sp)
                        Text(
                            text = pluralStringResource(R.plurals.settings_folder_apps_count, count, count),
                            color = p.dim,
                            fontSize = 12.sp,
                            fontFamily = launcherTechnicalFontFamily(),
                        )
                    }
                    Icon(painterResource(LauncherIcons.ChevronRight), contentDescription = null, tint = p.text)
                }
            }
        }
        FolderActionButton(
            label = stringResource(R.string.settings_new_drawer_folder),
            palette = p,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            onClick = onCreate,
        )
    }
}

@Composable
private fun FolderPage(
    folder: DrawerFolder,
    apps: List<AppItem>,
    hiddenKeys: Set<String>,
    otherMembers: Set<String>,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onSetMember: (key: String, member: Boolean) -> Unit,
) {
    val p = LocalExpressivePalette.current
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    var query by rememberSaveable(folder.id) { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val members = remember(folder.appKeys) { folder.appKeys.toSet() }
    val offered = remember(apps, hiddenKeys, otherMembers, members, query) {
        val needle = query.trim()
        apps.filter { app ->
            app.key !in hiddenKeys &&
                (app.key in members || app.key !in otherMembers) &&
                (needle.isEmpty() || app.label.contains(needle, ignoreCase = true))
        }
    }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        FolderTextField(
            value = name,
            onValueChange = {
                name = it
                onRename(it)
            },
            label = stringResource(R.string.settings_folder_name),
            palette = p,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .testTag(FOLDER_MANAGER_NAME_TAG),
        )
        FolderTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.settings_folder_search),
            palette = p,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(FOLDER_MANAGER_SEARCH_TAG),
        )
        if (offered.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_folder_no_results),
                color = p.dim,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items = offered, key = { it.key }) { app ->
                val member = app.key in members
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = member, role = Role.Switch, onValueChange = { onSetMember(app.key, it) })
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag(FOLDER_MANAGER_APP_ROW_TAG),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(appItem = app, labelColor = p.text, showLabel = false, iconSize = 40.dp)
                    Text(
                        text = app.label,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        color = p.text,
                        fontSize = 16.sp,
                    )
                    NothingSwitch(checked = member, onCheckedChange = { onSetMember(app.key, it) })
                }
            }
        }
        FolderActionButton(
            label = stringResource(R.string.settings_folder_delete),
            palette = p,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            secondary = true,
            onClick = { confirmDelete = true },
        )
    }
    if (confirmDelete) {
        FolderDeleteDialog(
            folderName = folder.name,
            palette = p,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FolderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    palette: ExpressivePalette,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = palette.dim,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontFamily = launcherTechnicalFontFamily(),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = palette.text,
                fontSize = 15.sp,
                fontFamily = launcherTechnicalFontFamily(),
            ),
            cursorBrush = SolidColor(palette.text),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surfaceHi)
                        .border(1.dp, palette.trackOff, RoundedCornerShape(6.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, color = palette.dim, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun FolderActionButton(
    label: String,
    palette: ExpressivePalette,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (secondary) androidx.compose.ui.graphics.Color.Transparent else palette.text
    val foreground = if (secondary) palette.text else palette.bg
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .then(
                if (secondary) Modifier.border(1.dp, palette.trackOff, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = foreground,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            fontFamily = launcherTechnicalFontFamily(),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FolderDeleteDialog(
    folderName: String,
    palette: ExpressivePalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = palette.surfaceHi,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_folder_delete_title),
                    color = palette.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.settings_folder_delete_text, folderName),
                    color = palette.dim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FolderActionButton(
                        label = stringResource(R.string.settings_folder_cancel),
                        palette = palette,
                        secondary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    FolderActionButton(
                        label = stringResource(R.string.settings_folder_delete_confirm),
                        palette = palette,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}
