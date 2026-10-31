package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.components.Base64AvatarImage
import com.example.ui.components.CompressedPhotoPickerButton
import com.example.ui.components.GlobalImageLightboxController
import com.example.ui.components.HonorHeartReactionButton
import com.example.ui.components.StudentActivitiesVectorIcon
import com.example.ui.theme.*
import com.example.util.ImageCompressor
import com.example.viewmodel.SchoolViewModel

private val SoftMintBg = Color(0xFFD1FAE5)
private val CardBorderColor = Color(0xFFA7F3D0).copy(alpha = 0.55f)
private val TextPrimaryColor = Color(0xFF0F172A)
private val TextSecondaryColor = Color(0xFF475569)

fun formatActivityAuthorRoleLabel(
    role: UserRole,
    gradeClass: String,
    authorUserId: String,
    accounts: List<UserAccount>
): String {
    return when (role) {
        UserRole.ADMIN -> "المدير"
        UserRole.TEACHER -> {
            val teacherAcc = accounts.firstOrNull { it.id == authorUserId }
            val spec = teacherAcc?.teacherSubjectsList?.joinToString("، ")?.ifBlank { teacherAcc.specialty }
                ?: ""
            if (spec.isNotBlank()) "معلم • $spec" else "معلم"
        }
        UserRole.STUDENT, UserRole.PARENT -> {
            if (gradeClass.isNotBlank()) "${role.arabicTitle} • $gradeClass" else role.arabicTitle
        }
    }
}

@Composable
fun VectorHeartIcon(
    isFilled: Boolean,
    tint: Color,
    size: Dp = 20.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.86f)
            cubicTo(
                w * 0.14f, h * 0.62f,
                w * 0.08f, h * 0.30f,
                w * 0.28f, h * 0.16f
            )
            cubicTo(
                w * 0.40f, h * 0.08f,
                w * 0.50f, h * 0.20f,
                w * 0.50f, h * 0.28f
            )
            cubicTo(
                w * 0.50f, h * 0.20f,
                w * 0.60f, h * 0.08f,
                w * 0.72f, h * 0.16f
            )
            cubicTo(
                w * 0.92f, h * 0.30f,
                w * 0.86f, h * 0.62f,
                w * 0.5f, h * 0.86f
            )
            close()
        }
        if (isFilled) {
            drawPath(path = path, color = tint)
        } else {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun VectorShieldModerationIcon(
    tint: Color,
    size: Dp = 18.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            lineTo(w * 0.86f, h * 0.24f)
            lineTo(w * 0.86f, h * 0.52f)
            cubicTo(w * 0.86f, h * 0.74f, w * 0.68f, h * 0.86f, w * 0.5f, h * 0.92f)
            cubicTo(w * 0.32f, h * 0.86f, w * 0.14f, h * 0.74f, w * 0.14f, h * 0.52f)
            lineTo(w * 0.14f, h * 0.24f)
            close()
        }
        drawPath(
            path = shield,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Clean inner check emblem (no vertical line)
        val checkPath = Path().apply {
            moveTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.64f)
            lineTo(w * 0.67f, h * 0.40f)
        }
        drawPath(
            path = checkPath,
            color = tint,
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun VectorFlagReportIcon(
    tint: Color,
    size: Dp = 16.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(
            color = tint,
            start = Offset(w * 0.24f, h * 0.14f),
            end = Offset(w * 0.24f, h * 0.88f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        val flag = Path().apply {
            moveTo(w * 0.24f, h * 0.16f)
            lineTo(w * 0.80f, h * 0.26f)
            lineTo(w * 0.66f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.58f)
            lineTo(w * 0.24f, h * 0.58f)
            close()
        }
        drawPath(
            path = flag,
            color = tint,
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ActivityBase64BannerImage(
    base64Data: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64Data) {
        ImageCompressor.decodeBase64ToImageBitmap(base64Data)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun StudentActivitiesScreen(viewModel: SchoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allPosts by viewModel.studentActivityPosts.collectAsState()
    val violationReports by viewModel.activityViolationReports.collectAsState()
    val profilesAndSanctionsMap by viewModel.userProfilesAndSanctions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val deepLinkTarget by viewModel.deepLinkTarget.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val canModerate = isAdmin || isTeacher
    val myUserId = currentUser?.id ?: ""

    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Feed, 1: Moderation Queue, 2: Reports (Admin)
    var selectedFeedFilter by remember { mutableIntStateOf(0) } // 0: Top Engagement, 1: Newest, 2: Following
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var highlightedPostId by remember { mutableStateOf<String?>(null) }

    // Modals state
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<StudentActivityPost?>(null) }
    var selectedProfileUser by remember { mutableStateOf<ProfileModalTarget?>(null) }
    var reportingTarget by remember { mutableStateOf<ReportModalTarget?>(null) }
    var sanctionTarget by remember { mutableStateOf<SanctionModalTarget?>(null) }

    // Handle deep link navigation
    LaunchedEffect(deepLinkTarget) {
        val target = deepLinkTarget
        if (target != null && target.section == AppSection.STUDENT_ACTIVITIES) {
            selectedMainTab = target.tabIndex.coerceIn(0, if (isAdmin) 2 else 1)
            if (target.itemId.isNotBlank()) {
                highlightedPostId = target.itemId
            }
            viewModel.clearDeepLinkTarget()
        }
    }

    val myProfile = remember(profilesAndSanctionsMap, myUserId) {
        viewModel.getUserActivityProfile(myUserId)
    }
    val followedIds = myProfile.followingUserIds.toSet()

    val approvedPosts = remember(allPosts, selectedFeedFilter, selectedCategoryFilter, followedIds) {
        val base = allPosts.filter { it.status == StudentActivityStatus.APPROVED }
            .filter { selectedCategoryFilter == "الكل" || it.category == selectedCategoryFilter }

        when (selectedFeedFilter) {
            1 -> base.sortedByDescending { it.createdAtMillis }
            2 -> base.filter { it.authorUserId in followedIds || it.authorUserId == myUserId }
                .sortedByDescending { it.engagementScore(it.authorUserId in followedIds) }
            else -> base.sortedByDescending { it.engagementScore(it.authorUserId in followedIds) }
        }
    }

    val pendingPosts = remember(allPosts, canModerate, myUserId) {
        if (canModerate) {
            allPosts.filter { it.status == StudentActivityStatus.PENDING }
                .sortedByDescending { it.createdAtMillis }
        } else {
            allPosts.filter { it.authorUserId == myUserId && it.status != StudentActivityStatus.APPROVED }
                .sortedByDescending { it.createdAtMillis }
        }
    }

    val unresolvedReportsCount = remember(violationReports) {
        violationReports.count { !it.isResolved }
    }

    val categories = remember {
        listOf(
            "الكل",
            "ابتكار ومشاريع علمية",
            "رسم وفنون جميلة",
            "مقالات وإبداع أدبي",
            "مبادرات طلابية",
            "تفوق وتجارب دراسية"
        )
    }

    var visibleFeedPostsCount by remember(selectedFeedFilter, selectedCategoryFilter) {
        mutableIntStateOf(15)
    }

    // Single unified LazyColumn so the header and filters scroll smoothly with the posts
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Luxury Emerald Header Banner inside unified LazyColumn
        item(key = "student_activities_header_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepEmerald),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DeepEmerald, EmeraldAccent, DeepForestGreen)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    StudentActivitiesVectorIcon(tint = GoldAccent, size = 26.dp)
                                }
                            }
                            Column {
                                Text(
                                    text = "مجتمع ونشاطات الطلبة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "منصة الإبداع العلمي، الفنون، والمشاريع الطلابية بإشراف تربوي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        // My Creative Profile Button
                        if (currentUser != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clickable {
                                        selectedProfileUser = ProfileModalTarget(
                                            userId = currentUser!!.id,
                                            userName = currentUser!!.fullName,
                                            userRole = currentUser!!.role,
                                            userGrade = if (currentUser!!.role == UserRole.STUDENT) currentUser!!.gradeClass else "",
                                            userPhotoBase64 = currentUser!!.photoBase64
                                        )
                                    }
                                    .testTag("my_creative_profile_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "ملفي الإبداعي",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ملفي الإبداعي",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Tabs Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            ActivityHeaderTabChip(
                                title = "ساحة الإبداع (${approvedPosts.size})",
                                selected = selectedMainTab == 0,
                                onClick = { selectedMainTab = 0 }
                            )
                        }

                        val pendingLabel = if (canModerate) {
                            "قائمة المراجعة (${pendingPosts.size})"
                        } else {
                            "منشوراتي قيد المراجعة (${pendingPosts.size})"
                        }
                        if (canModerate || pendingPosts.isNotEmpty()) {
                            item {
                                ActivityHeaderTabChip(
                                    title = pendingLabel,
                                    selected = selectedMainTab == 1,
                                    badgeHighlight = canModerate && pendingPosts.isNotEmpty(),
                                    onClick = { selectedMainTab = 1 }
                                )
                            }
                        }

                        if (isAdmin) {
                            item {
                                ActivityHeaderTabChip(
                                    title = "صندوق البلاغات ($unresolvedReportsCount)",
                                    selected = selectedMainTab == 2,
                                    badgeHighlight = unresolvedReportsCount > 0,
                                    onClick = { selectedMainTab = 2 }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sanction Warning Banner if current user is muted
                    if (myProfile.isPostingMuted() || myProfile.isInteractionMuted()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DangerRed.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VectorShieldModerationIcon(tint = Color(0xFFFFDAD6), size = 20.dp)
                                Column {
                                    Text(
                                        text = "تنبيه إداري على حسابك في مجتمع الطلبة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val statusMsg = buildString {
                                        if (myProfile.isPostingMuted()) {
                                            append("تم تعليق صلاحية النشر (${myProfile.remainingPostingBanLabel()}). ")
                                        }
                                        if (myProfile.isInteractionMuted()) {
                                            append("تم تعليق التفاعل والتعليقات (${myProfile.remainingInteractionBanLabel()}).")
                                        }
                                    }
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Publish New Activity Button
                    Button(
                        onClick = {
                            editingPost = null
                            showCreatePostDialog = true
                        },
                        enabled = !myProfile.isPostingMuted(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DeepForestGreen,
                            disabledContainerColor = Color.White.copy(alpha = 0.2f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("publish_student_activity_btn")
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (myProfile.isPostingMuted()) {
                                "النشر معلق مؤقتاً بقرار إداري (${myProfile.remainingPostingBanLabel()})"
                            } else if (canModerate) {
                                "نشر نشاط أو إبداع جديد (نشر فوري معتمد)"
                            } else {
                                "شارك إبداعك أو مشروعك العلمي (يُنشر بعد موافقة الإدارة)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Main Body inside the same unified LazyColumn according to selectedMainTab
        when (selectedMainTab) {
            0 -> {
                // Feed Sort & Category Filters (Smooth circular chips without sharp gray borders)
                item(key = "feed_filters_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortTabs = listOf(
                            "الأكثر تفاعلاً وتميزاً",
                            "الأحدث",
                            "منشورات من أتابعهم (${followedIds.size})"
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortTabs.size) { idx ->
                                val label = sortTabs[idx]
                                val isSelected = selectedFeedFilter == idx
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) DeepEmerald else SoftMintBg.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DeepEmerald else EmeraldAccent.copy(alpha = 0.30f)
                                    ),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedFeedFilter = idx }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else DeepEmerald,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Category Filter Row (Smooth Pill / CircleShape without gray outlines)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) GoldAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DeepEmerald else EmeraldAccent.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedCategoryFilter = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) DeepEmerald else TextSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (approvedPosts.isEmpty()) {
                    item(key = "empty_approved_posts") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StudentActivitiesVectorIcon(tint = DeepEmerald, size = 44.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لا توجد نشاطات مطابقة في هذا التصنيف حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "كن أول من يشارك مشروعاً علمياً أو موهبة إبداعية ليظهر لجميع طلبة ومعلمي المدرسة!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val displayedPosts = approvedPosts.take(visibleFeedPostsCount)
                    items(displayedPosts, key = { it.id }) { post ->
                        StudentActivityPostCard(
                            post = post,
                            currentUser = currentUser,
                            accounts = accounts,
                            isAuthorFollowed = post.authorUserId in followedIds,
                            isHighlighted = post.id == highlightedPostId,
                            isInteractionMuted = myProfile.isInteractionMuted(),
                            viewModel = viewModel,
                            onOpenAuthorProfile = { target ->
                                selectedProfileUser = target
                            },
                            onEditPost = {
                                editingPost = post
                                showCreatePostDialog = true
                            },
                            onReportTarget = { target ->
                                reportingTarget = target
                            },
                            onOpenAdminSanction = { target ->
                                sanctionTarget = target
                            }
                        )
                    }
                    if (approvedPosts.size > visibleFeedPostsCount) {
                        item(key = "load_more_posts_btn") {
                            OutlinedButton(
                                onClick = { visibleFeedPostsCount += 15 },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, DeepEmerald)
                            ) {
                                Text(
                                    text = "عرض المزيد من النشاطات (${approvedPosts.size - visibleFeedPostsCount} متبقية)",
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }

            1 -> {
                // Pending Moderation Queue (for Admin & Teacher, or Student's own pending posts)
                item(key = "moderation_queue_header") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftMintBg.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VectorShieldModerationIcon(tint = DeepEmerald, size = 22.dp)
                            Column {
                                Text(
                                    text = if (canModerate) {
                                        "قائمة مراجعة واعتماد نشاطات الطلبة"
                                    } else {
                                        "منشوراتك قيد المراجعة الإدارية"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepEmerald
                                )
                                Text(
                                    text = if (canModerate) {
                                        "راجع المشاركات الطلابية قبل ظهورها في الساحة العامة لضمان جودة المحتوى التربوي والعلمي."
                                    } else {
                                        "يتم فحص المنشورات من قبل إدارة المدرسة أو المعلم المشرف قبل نشرها للجميع."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryColor
                                )
                            }
                        }
                    }
                }

                if (pendingPosts.isEmpty()) {
                    item(key = "empty_pending_posts") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد منشورات معلقة بانتظار المراجعة حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    items(pendingPosts, key = { "pending_${it.id}" }) { post ->
                        PendingActivityModerationCard(
                            post = post,
                            accounts = accounts,
                            canModerate = canModerate,
                            isOwner = post.authorUserId == myUserId,
                            onApprove = {
                                viewModel.reviewStudentActivityPost(
                                    postId = post.id,
                                    approve = true
                                )
                            },
                            onReject = {
                                viewModel.reviewStudentActivityPost(
                                    postId = post.id,
                                    approve = false
                                )
                            },
                            onDelete = {
                                viewModel.deleteStudentActivityPost(post.id)
                            },
                            onSanctionAuthor = if (isAdmin) {
                                {
                                    sanctionTarget = SanctionModalTarget(
                                        userId = post.authorUserId,
                                        studentId = post.authorStudentId,
                                        userName = post.authorName,
                                        userRole = post.authorRole
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            2 -> {
                // Admin Violation Reports & Sanctions Center inside the unified LazyColumn
                item(key = "admin_violation_reports_section") {
                    AdminViolationReportsListSection(
                        reports = violationReports,
                        profilesAndSanctionsMap = profilesAndSanctionsMap,
                        accounts = accounts,
                        viewModel = viewModel,
                        onOpenSanctionModal = { target ->
                            sanctionTarget = target
                        }
                    )
                }
            }
        }
    }

    // Create / Edit Activity Post Modal
    if (showCreatePostDialog) {
        CreateOrEditStudentActivityDialog(
            initialPost = editingPost,
            categories = categories.filter { it != "الكل" },
            canPublishDirectly = canModerate,
            onDismiss = {
                showCreatePostDialog = false
                editingPost = null
            },
            onSubmit = { title, content, category, imageBase64 ->
                val existing = editingPost
                if (existing != null) {
                    viewModel.updateStudentActivityPost(
                        postId = existing.id,
                        category = category,
                        title = title,
                        content = content,
                        imageBase64 = imageBase64
                    )
                } else {
                    viewModel.publishStudentActivityPost(
                        category = category,
                        title = title,
                        content = content,
                        imageBase64 = imageBase64
                    )
                }
                showCreatePostDialog = false
                editingPost = null
            }
        )
    }

    // User Creative Profile Modal
    selectedProfileUser?.let { target ->
        UserCreativeProfileModalDialog(
            target = target,
            currentUser = currentUser,
            accounts = accounts,
            allPosts = allPosts,
            viewModel = viewModel,
            onDismiss = { selectedProfileUser = null },
            onEditPost = { post ->
                selectedProfileUser = null
                editingPost = post
                showCreatePostDialog = true
            },
            onOpenSanctionModal = { sanctionUsr ->
                selectedProfileUser = null
                sanctionTarget = sanctionUsr
            }
        )
    }

    // Report Violation Modal
    reportingTarget?.let { target ->
        SubmitViolationReportDialog(
            target = target,
            onDismiss = { reportingTarget = null },
            onSubmit = { reason ->
                viewModel.reportStudentActivityViolation(
                    postId = target.postId,
                    commentId = target.commentId,
                    replyId = target.replyId,
                    targetType = target.targetType,
                    reportedUserId = target.violatorId,
                    reportedStudentId = target.violatorStudentId,
                    reportedUserName = target.violatorName,
                    reportedUserRole = target.violatorRole,
                    contentExcerpt = target.contentPreview,
                    reason = reason
                )
                reportingTarget = null
            }
        )
    }

    // Admin Full Sanctions & Warning Modal
    sanctionTarget?.let { target ->
        AdminSanctionsAndWarningModalDialog(
            target = target,
            currentProfile = viewModel.getUserActivityProfile(target.userId),
            viewModel = viewModel,
            onDismiss = { sanctionTarget = null }
        )
    }
}

@Composable
private fun ActivityHeaderTabChip(
    title: String,
    selected: Boolean,
    badgeHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
        border = BorderStroke(
            1.dp,
            if (badgeHighlight && !selected) GoldAccent else Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (badgeHighlight) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected) DangerRed else GoldAccent)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) DeepEmerald else Color.White,
                maxLines = 1
            )
        }
    }
}

data class ProfileModalTarget(
    val userId: String,
    val userName: String,
    val userRole: UserRole,
    val userGrade: String,
    val userPhotoBase64: String
)

data class ReportModalTarget(
    val postId: String,
    val commentId: String = "",
    val replyId: String = "",
    val targetType: ViolationTargetType,
    val violatorId: String,
    val violatorStudentId: String = "",
    val violatorName: String,
    val violatorRole: UserRole,
    val contentPreview: String
)

data class SanctionModalTarget(
    val userId: String,
    val studentId: String = "",
    val userName: String,
    val userRole: UserRole,
    val relatedReportId: String = ""
)

@Composable
fun StudentActivityPostCard(
    post: StudentActivityPost,
    currentUser: UserAccount?,
    accounts: List<UserAccount>,
    isAuthorFollowed: Boolean,
    isHighlighted: Boolean,
    isInteractionMuted: Boolean,
    viewModel: SchoolViewModel,
    onOpenAuthorProfile: (ProfileModalTarget) -> Unit,
    onEditPost: () -> Unit,
    onReportTarget: (ReportModalTarget) -> Unit,
    onOpenAdminSanction: (SanctionModalTarget) -> Unit
) {
    val myUserId = currentUser?.id ?: ""
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isOwner = post.authorUserId == myUserId
    val canEditPost = isOwner
    val canDeletePost = isOwner || isAdmin
    val isLikedByMe = myUserId.isNotBlank() && post.likedByUserIds.contains(myUserId)
    val heartScale by animateFloatAsState(
        targetValue = if (isLikedByMe) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heart_scale"
    )

    var showCommentsSection by remember(isHighlighted) { mutableStateOf(isHighlighted) }
    var visibleCommentsCount by remember(post.id) { mutableIntStateOf(4) }
    var newCommentText by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var editingCommentValue by remember { mutableStateOf("") }
    var editingReplyKey by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editingReplyValue by remember { mutableStateOf("") }

    val formattedAuthorSubtitle = remember(post.authorRole, post.authorGradeClass, post.authorUserId, accounts, post.createdAt) {
        val rolePart = formatActivityAuthorRoleLabel(
            role = post.authorRole,
            gradeClass = post.authorGradeClass,
            authorUserId = post.authorUserId,
            accounts = accounts
        )
        "$rolePart • ${post.createdAt}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_post_${post.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) GoldAccent else CardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Author Row + Follow + Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onOpenAuthorProfile(
                                ProfileModalTarget(
                                    userId = post.authorUserId,
                                    userName = post.authorName,
                                    userRole = post.authorRole,
                                    userGrade = if (post.authorRole == UserRole.STUDENT) post.authorGradeClass else "",
                                    userPhotoBase64 = post.authorPhotoBase64
                                )
                            )
                        }
                ) {
                    Base64AvatarImage(
                        base64Data = post.authorPhotoBase64,
                        fallbackIcon = Icons.Filled.Person,
                        contentDescription = post.authorName,
                        size = 44.dp
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Follow Button (if not current user)
                            if (myUserId.isNotBlank() && post.authorUserId != myUserId) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isAuthorFollowed) SoftMintBg else DeepEmerald,
                                    border = BorderStroke(1.dp, DeepEmerald),
                                    modifier = Modifier.clickable {
                                        viewModel.toggleFollowUser(post.authorUserId)
                                    }
                                ) {
                                    Text(
                                        text = if (isAuthorFollowed) "متابَع" else "+ متابعة",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAuthorFollowed) DeepEmerald else Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = formattedAuthorSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Category Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftMintBg.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Title & Content
            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            // Post Attached Image (Clickable for Full-Screen Lightbox)
            if (post.imageBase64.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            GlobalImageLightboxController.open(
                                base64Data = post.imageBase64,
                                title = post.title.ifBlank { "نشاط الطالب: ${post.authorName}" }
                            )
                        }
                ) {
                    ActivityBase64BannerImage(
                        base64Data = post.imageBase64,
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorderColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            // Interaction & Moderation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Heart Button identical to Honor Roll & Events Gallery (HonorHeartReactionButton)
                    HonorHeartReactionButton(
                        heartCount = post.heartCount,
                        isLikedByMe = isLikedByMe,
                        onReact = {
                            if (!isInteractionMuted) {
                                viewModel.toggleStudentActivityHeart(post.id)
                            }
                        },
                        testTag = "like_post_${post.id}"
                    )

                    // Comments Toggle Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (showCommentsSection) SoftMintBg.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (showCommentsSection) DeepEmerald else CardBorderColor
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showCommentsSection = !showCommentsSection }
                            .testTag("comments_toggle_${post.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "التعليقات",
                                tint = DeepEmerald,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "التعليقات (${post.totalCommentsAndRepliesCount})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                        }
                    }
                }

                // Right Side: Edit (Owner ONLY) / Delete (Owner or Admin) / Report / Admin Sanction
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canEditPost) {
                        IconButton(
                            onClick = onEditPost,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل المنشور",
                                tint = DeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (canDeletePost) {
                        IconButton(
                            onClick = { viewModel.deleteStudentActivityPost(post.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "حذف المنشور",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!isOwner) {
                        // Report Violation Button
                        IconButton(
                            onClick = {
                                onReportTarget(
                                    ReportModalTarget(
                                        postId = post.id,
                                        targetType = ViolationTargetType.POST,
                                        violatorId = post.authorUserId,
                                        violatorStudentId = post.authorStudentId,
                                        violatorName = post.authorName,
                                        violatorRole = post.authorRole,
                                        contentPreview = "${post.title} - ${post.content}".take(120)
                                    )
                                )
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            VectorFlagReportIcon(tint = GoldDeep, size = 17.dp)
                        }
                    }

                    if (isAdmin && !isOwner) {
                        // Admin Direct Sanction Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DangerRed.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                onOpenAdminSanction(
                                    SanctionModalTarget(
                                        userId = post.authorUserId,
                                        studentId = post.authorStudentId,
                                        userName = post.authorName,
                                        userRole = post.authorRole
                                    )
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                VectorShieldModerationIcon(tint = DangerRed, size = 14.dp)
                                Text(
                                    text = "إجراء رقابي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Comments & Nested Replies Section
            AnimatedVisibility(visible = showCommentsSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "التعليقات والمناقشات العلمية (${post.totalCommentsAndRepliesCount})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )

                    if (post.comments.isEmpty()) {
                        Text(
                            text = "لا توجد تعليقات بعد. أضف تعليقاً مشجعاً أو سؤالاً علمياً!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val displayedComments = post.comments.take(visibleCommentsCount)
                        displayedComments.forEach { comment ->
                            val isCommentOwner = comment.authorUserId == myUserId
                            val canEditComment = isCommentOwner
                            val canDeleteComment = isCommentOwner || isAdmin
                            val commentRoleLabel = formatActivityAuthorRoleLabel(
                                role = comment.authorRole,
                                gradeClass = comment.authorGradeClass,
                                authorUserId = comment.authorUserId,
                                accounts = accounts
                            )

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    onOpenAuthorProfile(
                                                        ProfileModalTarget(
                                                            userId = comment.authorUserId,
                                                            userName = comment.authorName,
                                                            userRole = comment.authorRole,
                                                            userGrade = if (comment.authorRole == UserRole.STUDENT) comment.authorGradeClass else "",
                                                            userPhotoBase64 = comment.authorPhotoBase64
                                                        )
                                                    )
                                                }
                                        ) {
                                            Text(
                                                text = comment.authorName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepEmerald
                                            )
                                            Text(
                                                text = "• $commentRoleLabel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isInteractionMuted) {
                                                Text(
                                                    text = "رد",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RoyalGreen,
                                                    modifier = Modifier.clickable {
                                                        replyingToCommentId =
                                                            if (replyingToCommentId == comment.id) null else comment.id
                                                        replyText = ""
                                                    }
                                                )
                                            }
                                            if (canEditComment) {
                                                Text(
                                                    text = "تعديل",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepEmerald,
                                                    modifier = Modifier.clickable {
                                                        editingCommentId = comment.id
                                                        editingCommentValue = comment.text
                                                    }
                                                )
                                            }
                                            if (canDeleteComment) {
                                                Text(
                                                    text = "حذف",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DangerRed,
                                                    modifier = Modifier.clickable {
                                                        viewModel.deleteStudentActivityComment(
                                                            postId = post.id,
                                                            commentId = comment.id
                                                        )
                                                    }
                                                )
                                            }
                                            if (!isCommentOwner) {
                                                Box(
                                                    modifier = Modifier.clickable {
                                                        onReportTarget(
                                                            ReportModalTarget(
                                                                postId = post.id,
                                                                commentId = comment.id,
                                                                targetType = ViolationTargetType.COMMENT,
                                                                violatorId = comment.authorUserId,
                                                                violatorName = comment.authorName,
                                                                violatorRole = comment.authorRole,
                                                                contentPreview = comment.text
                                                            )
                                                        )
                                                    }
                                                ) {
                                                    VectorFlagReportIcon(tint = GoldDeep, size = 14.dp)
                                                }
                                            }
                                            if (isAdmin && !isCommentOwner) {
                                                Box(
                                                    modifier = Modifier.clickable {
                                                        onOpenAdminSanction(
                                                            SanctionModalTarget(
                                                                userId = comment.authorUserId,
                                                                userName = comment.authorName,
                                                                userRole = comment.authorRole
                                                            )
                                                        )
                                                    }
                                                ) {
                                                    VectorShieldModerationIcon(tint = DangerRed, size = 14.dp)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (editingCommentId == comment.id) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editingCommentValue,
                                                onValueChange = { editingCommentValue = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.updateStudentActivityComment(
                                                        postId = post.id,
                                                        commentId = comment.id,
                                                        newText = editingCommentValue
                                                    )
                                                    editingCommentId = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                                            ) {
                                                Text("حفظ", fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Nested Replies inside this Comment
                                    if (comment.replies.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, start = 14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            comment.replies.forEach { reply ->
                                                val isReplyOwner = reply.authorUserId == myUserId
                                                val canEditReply = isReplyOwner
                                                val canDeleteReply = isReplyOwner || isAdmin
                                                val replyRoleLabel = formatActivityAuthorRoleLabel(
                                                    role = reply.authorRole,
                                                    gradeClass = "",
                                                    authorUserId = reply.authorUserId,
                                                    accounts = accounts
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = SoftMintBg.copy(alpha = 0.35f),
                                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.25f))
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "${reply.authorName} ($replyRoleLabel)",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = DeepEmerald,
                                                                fontSize = 11.sp
                                                            )

                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                if (canEditReply) {
                                                                    Text(
                                                                        text = "تعديل",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = DeepEmerald,
                                                                        fontSize = 10.sp,
                                                                        modifier = Modifier.clickable {
                                                                            editingReplyKey = comment.id to reply.id
                                                                            editingReplyValue = reply.text
                                                                        }
                                                                    )
                                                                }
                                                                if (canDeleteReply) {
                                                                    Text(
                                                                        text = "حذف",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = DangerRed,
                                                                        fontSize = 10.sp,
                                                                        modifier = Modifier.clickable {
                                                                            viewModel.deleteStudentActivityReply(
                                                                                postId = post.id,
                                                                                commentId = comment.id,
                                                                                replyId = reply.id
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                                if (!isReplyOwner) {
                                                                    Box(
                                                                        modifier = Modifier.clickable {
                                                                            onReportTarget(
                                                                                ReportModalTarget(
                                                                                    postId = post.id,
                                                                                    commentId = comment.id,
                                                                                    replyId = reply.id,
                                                                                    targetType = ViolationTargetType.REPLY,
                                                                                    violatorId = reply.authorUserId,
                                                                                    violatorName = reply.authorName,
                                                                                    violatorRole = reply.authorRole,
                                                                                    contentPreview = reply.text
                                                                                )
                                                                            )
                                                                        }
                                                                    ) {
                                                                        VectorFlagReportIcon(
                                                                            tint = GoldDeep,
                                                                            size = 13.dp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (editingReplyKey == (comment.id to reply.id)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = editingReplyValue,
                                                                    onValueChange = { editingReplyValue = it },
                                                                    modifier = Modifier.weight(1f),
                                                                    singleLine = true
                                                                )
                                                                Button(
                                                                    onClick = {
                                                                        viewModel.updateStudentActivityReply(
                                                                            postId = post.id,
                                                                            commentId = comment.id,
                                                                            replyId = reply.id,
                                                                            newText = editingReplyValue
                                                                        )
                                                                        editingReplyKey = null
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = DeepEmerald
                                                                    )
                                                                ) {
                                                                    Text("حفظ", fontSize = 10.sp)
                                                                }
                                                            }
                                                        } else {
                                                            Text(
                                                                text = reply.text,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Reply Input Box for this comment
                                    if (replyingToCommentId == comment.id && !isInteractionMuted) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = replyText,
                                                onValueChange = { replyText = it },
                                                placeholder = {
                                                    Text(
                                                        "اكتب رداً على ${comment.authorName}...",
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (replyText.isNotBlank()) {
                                                        viewModel.addStudentActivityReply(
                                                            postId = post.id,
                                                            commentId = comment.id,
                                                            replyText = replyText
                                                        )
                                                        replyText = ""
                                                        replyingToCommentId = null
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "إرسال الرد",
                                                    tint = DeepEmerald
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (post.comments.size > visibleCommentsCount) {
                            TextButton(
                                onClick = { visibleCommentsCount += 6 },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "عرض المزيد من التعليقات (${post.comments.size - visibleCommentsCount} متبقية)",
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Add New Top-Level Comment Input
                    if (!isInteractionMuted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = {
                                    Text("أضف تعليقاً مشجعاً أو مداخلة علمية...", fontSize = 12.sp)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_${post.id}"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        viewModel.addStudentActivityComment(
                                            postId = post.id,
                                            commentText = newCommentText
                                        )
                                        newCommentText = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال التعليق",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم تعليق صلاحية إضافة التعليقات والردود على حسابك مؤقتاً.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingActivityModerationCard(
    post: StudentActivityPost,
    accounts: List<UserAccount>,
    canModerate: Boolean,
    isOwner: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit,
    onSanctionAuthor: (() -> Unit)?
) {
    val roleSubtitle = formatActivityAuthorRoleLabel(
        role = post.authorRole,
        gradeClass = post.authorGradeClass,
        authorUserId = post.authorUserId,
        accounts = accounts
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            when (post.status) {
                StudentActivityStatus.PENDING -> GoldAccent
                StudentActivityStatus.REJECTED -> DangerRed.copy(alpha = 0.5f)
                StudentActivityStatus.APPROVED -> EmeraldAccent
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    Text(
                        text = "$roleSubtitle • ${post.createdAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (post.status) {
                        StudentActivityStatus.PENDING -> GoldAccent.copy(alpha = 0.2f)
                        StudentActivityStatus.REJECTED -> DangerRed.copy(alpha = 0.12f)
                        StudentActivityStatus.APPROVED -> SoftMintBg
                    }
                ) {
                    Text(
                        text = post.status.arabicLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (post.status) {
                            StudentActivityStatus.PENDING -> GoldDeep
                            StudentActivityStatus.REJECTED -> DangerRed
                            StudentActivityStatus.APPROVED -> DeepEmerald
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "[${post.category}] ${post.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.imageBase64.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            GlobalImageLightboxController.open(post.imageBase64, post.title)
                        }
                ) {
                    ActivityBase64BannerImage(
                        base64Data = post.imageBase64,
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (canModerate && post.status == StudentActivityStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد ونشر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        border = BorderStroke(1.dp, DangerRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفض المنشور", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (onSanctionAuthor != null) {
                        IconButton(onClick = onSanctionAuthor) {
                            VectorShieldModerationIcon(tint = DangerRed, size = 20.dp)
                        }
                    }
                }
            } else if (isOwner) {
                OutlinedButton(
                    onClick = onDelete,
                    border = BorderStroke(1.dp, DangerRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("حذف هذا المنشور", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminViolationReportsListSection(
    reports: List<StudentActivityViolationReport>,
    profilesAndSanctionsMap: Map<String, UserActivityProfileAndSanctions>,
    accounts: List<UserAccount>,
    viewModel: SchoolViewModel,
    onOpenSanctionModal: (SanctionModalTarget) -> Unit
) {
    val activeSanctionedProfiles = remember(profilesAndSanctionsMap) {
        profilesAndSanctionsMap.values.filter {
            it.isPostingMuted() || it.isInteractionMuted() || it.warningCount > 0
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Sanctions Summary Card
        if (activeSanctionedProfiles.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "الحسابات الخاضعة لإجراءات رقابية أو إنذارات (${activeSanctionedProfiles.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    activeSanctionedProfiles.forEach { prof ->
                        val matchedAcc = accounts.firstOrNull { it.id == prof.userId }
                        val displayUserName = matchedAcc?.fullName ?: prof.userId
                        val displayRole = matchedAcc?.role?.arabicTitle ?: "عضو"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$displayUserName ($displayRole)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = buildString {
                                        if (prof.isPostingMuted()) {
                                            append("النشر: ${prof.remainingPostingBanLabel()} • ")
                                        }
                                        if (prof.isInteractionMuted()) {
                                            append("التفاعل: ${prof.remainingInteractionBanLabel()} • ")
                                        }
                                        append("عدد الإنذارات: ${prof.warningCount}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DangerRed,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.liftAdminUserSanctions(prof.userId, displayUserName)
                                },
                                border = BorderStroke(1.dp, DeepEmerald),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("رفع الحظر", fontSize = 11.sp, color = DeepEmerald)
                            }
                        }
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VectorShieldModerationIcon(tint = DeepEmerald, size = 40.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "صندوق بلاغات نشاطات الطلبة خالٍ من المخالفات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            reports.forEach { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (report.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (report.isResolved) CardBorderColor else DangerRed.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بلاغ عن ${report.targetType.arabicLabel} • ضد: ${report.reportedUserName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (report.isResolved) TextSecondaryColor else DangerRed
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (report.isResolved) SoftMintBg else DangerRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (report.isResolved) "تمت المعالجة" else "بلاغ جديد",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.isResolved) DeepEmerald else DangerRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "المُبلِّغ: ${report.reporterName} • ${report.createdAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سبب البلاغ: ${report.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "المحتوى المُبلَّغ عنه: \"${report.reportedContentExcerpt}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        if (report.resolutionActionNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الإجراء المتخذ: ${report.resolutionActionNote}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onOpenSanctionModal(
                                        SanctionModalTarget(
                                            userId = report.reportedUserId,
                                            studentId = report.reportedStudentId,
                                            userName = report.reportedUserName,
                                            userRole = report.reportedUserRole,
                                            relatedReportId = report.id
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("اتخاذ عقوبة / إنذار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    when (report.targetType) {
                                        ViolationTargetType.POST -> viewModel.deleteStudentActivityPost(report.postId)
                                        ViolationTargetType.COMMENT -> viewModel.deleteStudentActivityComment(
                                            report.postId,
                                            report.commentId
                                        )
                                        ViolationTargetType.REPLY -> viewModel.deleteStudentActivityReply(
                                            report.postId,
                                            report.commentId,
                                            report.replyId
                                        )
                                    }
                                    viewModel.deleteActivityViolationReport(report.id)
                                },
                                border = BorderStroke(1.dp, DeepEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حذف المحتوى وإغلاق البلاغ", fontSize = 11.sp, color = DeepEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateOrEditStudentActivityDialog(
    initialPost: StudentActivityPost?,
    categories: List<String>,
    canPublishDirectly: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, category: String, imageBase64: String) -> Unit
) {
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var content by remember { mutableStateOf(initialPost?.content ?: "") }
    var selectedCategory by remember {
        mutableStateOf(initialPost?.category ?: categories.firstOrNull() ?: "ابتكار ومشاريع علمية")
    }
    var attachedImageBase64 by remember { mutableStateOf(initialPost?.imageBase64 ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialPost != null) "تعديل المنشور الإبداعي" else "مشاركة نشاط أو إبداع طلابي جديد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )

                if (!canPublishDirectly && initialPost == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftMintBg.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "ملاحظة: سيُرسل منشورك إلى قائمة مراجعة الإدارة والمعلمين لاعتماده قبل ظهوره لجميع الطلبة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEmerald,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text(
                    text = "تصنيف النشاط:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepEmerald,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان النشاط أو المشروع العلمي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("تفاصيل الفكرة، التجربة، أو المشاركة الإبداعية...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                // Compressed HD Photo Attachment
                CompressedPhotoPickerButton(
                    label = if (attachedImageBase64.isBlank()) {
                        "إرفاق صورة عالية الوضوح للمشروع أو الرسمة (اختياري)"
                    } else {
                        "تغيير الصورة المرفقة"
                    },
                    currentBase64 = attachedImageBase64,
                    onImageCompressed = { b64 ->
                        attachedImageBase64 = b64
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (attachedImageBase64.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        ActivityBase64BannerImage(
                            base64Data = attachedImageBase64,
                            contentDescription = "معاينة الصورة",
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { attachedImageBase64 = "" },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إزالة الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (content.isNotBlank() || title.isNotBlank()) {
                                onSubmit(
                                    title.ifBlank { selectedCategory },
                                    content,
                                    selectedCategory,
                                    attachedImageBase64
                                )
                            }
                        },
                        enabled = content.isNotBlank() || title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (initialPost != null) "حفظ التعديلات" else "نشر المشاركة",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCreativeProfileModalDialog(
    target: ProfileModalTarget,
    currentUser: UserAccount?,
    accounts: List<UserAccount>,
    allPosts: List<StudentActivityPost>,
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit,
    onEditPost: (StudentActivityPost) -> Unit,
    onOpenSanctionModal: (SanctionModalTarget) -> Unit
) {
    val myUserId = currentUser?.id ?: ""
    val isMyOwnProfile = target.userId == myUserId
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val profileData = viewModel.getUserActivityProfile(target.userId)
    val myProfileData = viewModel.getUserActivityProfile(myUserId)
    val isFollowing = myProfileData.followingUserIds.contains(target.userId)

    val userPosts = remember(allPosts, target.userId, isMyOwnProfile, isAdmin) {
        allPosts.filter {
            it.authorUserId == target.userId &&
                (it.status == StudentActivityStatus.APPROVED || isMyOwnProfile || isAdmin)
        }.sortedByDescending { it.createdAtMillis }
    }

    val totalHeartsReceived = remember(userPosts) {
        userPosts.sumOf { it.heartCount }
    }
    val followersCount = viewModel.getFollowersCount(target.userId)

    var isEditingBio by remember { mutableStateOf(false) }
    var bioInput by remember(profileData.bio) { mutableStateOf(profileData.bio) }
    val profileRoleSubtitle = remember(target, accounts) {
        formatActivityAuthorRoleLabel(
            role = target.userRole,
            gradeClass = target.userGrade,
            authorUserId = target.userId,
            accounts = accounts
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Profile Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DeepEmerald, EmeraldAccent)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الملف الإبداعي في مجتمع المدرسة",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White
                                )
                            }
                        }

                        // Avatar
                        Base64AvatarImage(
                            base64Data = target.userPhotoBase64,
                            fallbackIcon = Icons.Filled.Person,
                            contentDescription = target.userName,
                            size = 72.dp,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = target.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = profileRoleSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatItem(count = "${userPosts.size}", label = "المنشورات")
                            ProfileStatItem(count = "$totalHeartsReceived", label = "إعجابات")
                            ProfileStatItem(count = "$followersCount", label = "متابعون")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Follow or Admin Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isMyOwnProfile && myUserId.isNotBlank()) {
                                Button(
                                    onClick = { viewModel.toggleFollowUser(target.userId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color.White else GoldAccent,
                                        contentColor = DeepForestGreen
                                    ),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (isFollowing) "إلغاء المتابعة" else "+ متابعة الحساب",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (isAdmin && !isMyOwnProfile) {
                                OutlinedButton(
                                    onClick = {
                                        onOpenSanctionModal(
                                            SanctionModalTarget(
                                                userId = target.userId,
                                                userName = target.userName,
                                                userRole = target.userRole
                                            )
                                        )
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFFDAD6)
                                    ),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("إجراء رقابي / العقوبات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bio Card + User Posts List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "النبذة التعريفية (Bio)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepEmerald
                                    )
                                    if (isMyOwnProfile) {
                                        Text(
                                            text = if (isEditingBio) "إلغاء" else "تعديل النبذة",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalGreen,
                                            modifier = Modifier.clickable {
                                                isEditingBio = !isEditingBio
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isEditingBio && isMyOwnProfile) {
                                    OutlinedTextField(
                                        value = bioInput,
                                        onValueChange = { bioInput = it },
                                        label = { Text("اكتب نبذة عن اهتماماتك العلمية ومواهبك...") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateMyActivityBio(bioInput)
                                            isEditingBio = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("حفظ النبذة التعريفية", fontSize = 12.sp)
                                    }
                                } else {
                                    Text(
                                        text = profileData.bio.ifBlank {
                                            if (isMyOwnProfile) "اضغط على (تعديل النبذة) لكتابة نبذة تعريفية عن مواهبك واهتماماتك العلمية."
                                            else "عضو مبدع في مجتمع ونشاطات الطلبة."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "منشورات ومشاركات الحساب (${userPosts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )
                    }

                    if (userPosts.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد منشورات لهذا الحساب حتى الآن.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(userPosts, key = { it.id }) { post ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${post.title} • ${post.createdAt}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepEmerald,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isMyOwnProfile || isAdmin) {
                                            Row {
                                                if (isMyOwnProfile) {
                                                    IconButton(
                                                        onClick = { onEditPost(post) },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "تعديل",
                                                            tint = DeepEmerald,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteStudentActivityPost(post.id)
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف",
                                                        tint = DangerRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = post.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = GoldAccent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun SubmitViolationReportDialog(
    target: ReportModalTarget,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val presetReasons = listOf(
        "محتوى غير لائق أو يخالف الآداب المدرسية",
        "تنمر أو إساءة للزملاء أو الكادر التدريسي",
        "معلومات خاطئة أو خارج الإطار العلمي والتربوي",
        "تكرار مزعج أو محتوى غير هادف"
    )
    var selectedReason by remember { mutableStateOf(presetReasons.first()) }
    var customNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إبلاغ إدارة المدرسة عن مخالفة (${target.targetType.arabicLabel})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = DangerRed
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "صاحب المحتوى: ${target.violatorName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                presetReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("توضيح إضافي لمدير المدرسة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullReason = if (customNotes.isNotBlank()) {
                        "$selectedReason — $customNotes"
                    } else selectedReason
                    onSubmit(fullReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("إرسال البلاغ للإدارة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AdminSanctionsAndWarningModalDialog(
    target: SanctionModalTarget,
    currentProfile: UserActivityProfileAndSanctions,
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    var selectedPostingMute by remember { mutableStateOf<SanctionDurationOption?>(null) }
    var selectedInteractionMute by remember { mutableStateOf<SanctionDurationOption?>(null) }
    var sendOfficialWarning by remember { mutableStateOf(true) }
    var warningMessage by remember {
        mutableStateOf("يرجى الالتزام بضوابط النشر التربوي والأخلاقي في مجتمع ونشاطات الطلبة.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VectorShieldModerationIcon(tint = DangerRed, size = 22.dp)
                Text(
                    text = "لوحة الإجراءات الرقابية والعقوبات الإدارية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "المستخدم: ${target.userName} (${target.userRole.arabicTitle})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (currentProfile.isPostingMuted() || currentProfile.isInteractionMuted()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الحساب مقيد حالياً بعقوبة نشطة",
                                style = MaterialTheme.typography.labelSmall,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    viewModel.liftAdminUserSanctions(target.userId, target.userName)
                                    onDismiss()
                                }
                            ) {
                                Text("رفع جميع القيود فوراً", fontWeight = FontWeight.ExtraBold, color = DeepEmerald)
                            }
                        }
                    }
                }

                // 1) Mute Posting
                Text(
                    text = "1. تعليق الحساب من النشر (Mute Posting):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedPostingMute == null,
                            onClick = { selectedPostingMute = null },
                            label = { Text("بدون تعليق", fontSize = 11.sp) }
                        )
                    }
                    items(SanctionDurationOption.entries) { option ->
                        FilterChip(
                            selected = selectedPostingMute == option,
                            onClick = { selectedPostingMute = option },
                            label = { Text(option.arabicLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // 2) Mute Interaction (Likes & Comments)
                Text(
                    text = "2. تعليق الحساب من التفاعل والتعليقات (Mute Likes & Comments):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedInteractionMute == null,
                            onClick = { selectedInteractionMute = null },
                            label = { Text("بدون تعليق", fontSize = 11.sp) }
                        )
                    }
                    items(SanctionDurationOption.entries) { option ->
                        FilterChip(
                            selected = selectedInteractionMute == option,
                            onClick = { selectedInteractionMute = option },
                            label = { Text(option.arabicLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // 3) Official Warning Notification
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "3. إرسال إنذار مخالفة رسمي (يصل للطالب وولي أمره):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sendOfficialWarning,
                        onCheckedChange = { sendOfficialWarning = it }
                    )
                }
                OutlinedTextField(
                    value = warningMessage,
                    onValueChange = { warningMessage = it },
                    label = { Text("نص الإنذار الإداري الرسمي وسبب الإجراء") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.applyAdminUserSanction(
                        targetUserId = target.userId,
                        targetStudentId = target.studentId,
                        targetUserName = target.userName,
                        mutePostingDuration = selectedPostingMute,
                        muteInteractionDuration = selectedInteractionMute,
                        sendOfficialWarning = sendOfficialWarning,
                        reasonAndViolationText = warningMessage,
                        relatedReportId = target.relatedReportId
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("تطبيق الإجراء الرقابي وإرسال الإنذار", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
