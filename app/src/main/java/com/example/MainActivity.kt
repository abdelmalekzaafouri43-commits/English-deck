package com.example

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import com.example.ai.AiTutorService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoVibrant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceBackground
import com.example.ui.theme.SpaceCard
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import kotlinx.coroutines.launch

data class SlideInfo(
  val id: Int,
  val title: String,
  val icon: String,
  val subtitle: String,
  val pedagogyTip: String
)

data class LessonNote(
  val title: String,
  val time: String,
  val objective: String,
  val studentActivity: String,
  val teacherTip: String
)

data class ChatMessage(
  val sender: String,
  val text: String
)

class MainActivity : ComponentActivity() {

  private var webViewRef: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        EnglishDeckApp(
          onRegisterWebView = { webViewRef = it },
          onEvalJs = { code -> webViewRef?.evaluateJavascript(code, null) }
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EnglishDeckApp(
  onRegisterWebView: (WebView) -> Unit,
  onEvalJs: (String) -> Unit
) {
  var currentSlideIndex by remember { mutableIntStateOf(0) }
  var currentVisibleFragments by remember { mutableIntStateOf(0) }
  var currentTotalFragments by remember { mutableIntStateOf(0) }
  var isAutoPlay by remember { mutableStateOf(false) }
  var autoPlayIntervalMs by remember { mutableStateOf(15000L) }
  var isLaserActive by remember { mutableStateOf(false) }
  var isDrawActive by remember { mutableStateOf(false) }
  var showNotesDialog by remember { mutableStateOf(false) }
  var showAiTutorDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val autoPlayProgress = remember { Animatable(0f) }
  LaunchedEffect(isAutoPlay, currentSlideIndex, autoPlayIntervalMs) {
    if (isAutoPlay) {
      autoPlayProgress.snapTo(0f)
      autoPlayProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = autoPlayIntervalMs.toInt(), easing = LinearEasing)
      )
    } else {
      autoPlayProgress.snapTo(0f)
    }
  }

  val slides = remember {
    listOf(
      SlideInfo(
        id = 0,
        title = "Cover & Intro",
        icon = "⚡",
        subtitle = "Small island to global superpower",
        pedagogyTip = "Warm up the class: Ask students how many English words they use when playing games or watching TikTok!"
      ),
      SlideInfo(
        id = 1,
        title = "Origins & French Twist",
        icon = "⚔️",
        subtitle = "Vikings & Cow vs. Beef",
        pedagogyTip = "Engage the room: Why do we say 'cow' on a farm but 'beef' on our plate? Connect Anglo-Saxon farmers to Norman royalty!"
      ),
      SlideInfo(
        id = 2,
        title = "Evolution Timeline",
        icon = "⏳",
        subtitle = "Old English to Modern Digital",
        pedagogyTip = "Timeline activity: Compare the Beowulf ancient chant with Chaucer, Shakespeare, and modern digital gaming slang!"
      ),
      SlideInfo(
        id = 3,
        title = "Why Everyone Speaks It",
        icon = "🚀",
        subtitle = "Gaming, Code & #1 Lingua Franca",
        pedagogyTip = "Ask students: For every 1 native English speaker, how many non-native speakers exist? (Answer: 3!)"
      ),
      SlideInfo(
        id = 4,
        title = "English is Weird!",
        icon = "🧠",
        subtitle = "Shortest sentence & Ghost words",
        pedagogyTip = "Challenge the class to pronounce 'Pneumonoultramicroscopicsilicovolcanoconiosis' without breathing!"
      ),
      SlideInfo(
        id = 5,
        title = "Interactive Quiz",
        icon = "🔥",
        subtitle = "Most common English letter",
        pedagogyTip = "Take a vote for letters A, B, C, D before tapping 'Reveal'! Watch for excitement when 'E' lights up green."
      ),
      SlideInfo(
        id = 6,
        title = "Conquer The World",
        icon = "🌍",
        subtitle = "The language belongs to YOU",
        pedagogyTip = "Inspire students: English is not about memorizing test rules; it's about connecting with 1.5 billion people."
      )
    )
  }

  // Back handler advances backwards through presentation
  BackHandler(enabled = currentSlideIndex > 0 || currentVisibleFragments > 0) {
    onEvalJs("window.deck.prev();")
  }

  // Main UI: The Dashboard is ALWAYS on the left of the screen
  Row(
    modifier = Modifier
      .fillMaxSize()
      .background(SpaceBackground)
  ) {
    // ----------------------------------------------------
    // LEFT DASHBOARD (Always on the left of the screen)
    // ----------------------------------------------------
    LeftDashboard(
      slides = slides,
      currentSlide = currentSlideIndex,
      visibleFragments = currentVisibleFragments,
      totalFragments = currentTotalFragments,
      isAutoPlay = isAutoPlay,
      autoPlayIntervalMs = autoPlayIntervalMs,
      isLaserActive = isLaserActive,
      isDrawActive = isDrawActive,
      onSelectSlide = { slideIndex ->
        onEvalJs("window.deck.goToSlide($slideIndex, true);")
      },
      onNext = {
        onEvalJs("window.deck.next();")
      },
      onPrev = {
        onEvalJs("window.deck.prev();")
      },
      onToggleAutoPlay = {
        onEvalJs("window.deck.toggleAutoPlay();")
      },
      onChangeAutoPlaySpeed = { ms ->
        autoPlayIntervalMs = ms
        onEvalJs("window.deck.setAutoPlaySpeed($ms);")
      },
      onToggleLaser = {
        isLaserActive = !isLaserActive
        if (isLaserActive) isDrawActive = false
        onEvalJs("window.deck.toggleLaser();")
      },
      onToggleDraw = {
        isDrawActive = !isDrawActive
        if (isDrawActive) isLaserActive = false
        onEvalJs("window.deck.toggleDraw();")
      },
      onClearDraw = {
        onEvalJs("window.deck.clearDrawing();")
      },
      onPronounceWord = {
        onEvalJs("window.deck.pronounceKeyWord();")
      },
      onSendReaction = { emoji ->
        onEvalJs("window.deck.sendReaction('$emoji');")
      },
      onOpenNotes = {
        showNotesDialog = true
        onEvalJs("window.deck.openNotes();")
      },
      onOpenAiTutor = {
        showAiTutorDialog = true
      },
      onRevealQuiz = {
        onEvalJs("window.deck.revealQuiz();")
      },
      onCelebrate = {
        onEvalJs("window.deck.triggerConfetti();")
      },
      onReset = {
        onEvalJs("window.deck.resetDeck();")
      },
      onToggleSound = {
        onEvalJs("window.deck.toggleSound();")
      },
      modifier = Modifier
        .width(185.dp)
        .fillMaxHeight()
        .testTag("left_dashboard_container")
    )

    // ----------------------------------------------------
    // RIGHT PRESENTATION STAGE (WebView Loading Deck)
    // ----------------------------------------------------
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .background(Color(0xFF04060E))
        .padding(6.dp),
      contentAlignment = Alignment.Center
    ) {
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, SpaceCardBorder, RoundedCornerShape(16.dp))
          .testTag("presentation_webview"),
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = false

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {}

            // Bridge to sync WebView presentation state with Compose Dashboard
            addJavascriptInterface(object {
              @JavascriptInterface
              fun onSlideChanged(slide: Int, total: Int, visibleFrag: Int, totalFrag: Int) {
                scope.launch {
                  currentSlideIndex = slide
                  currentVisibleFragments = visibleFrag
                  currentTotalFragments = totalFrag
                }
              }

              @JavascriptInterface
              fun onAutoPlayChanged(active: Boolean) {
                scope.launch {
                  isAutoPlay = active
                }
              }

              @JavascriptInterface
              fun onLaserChanged(active: Boolean) {
                scope.launch {
                  isLaserActive = active
                }
              }

              @JavascriptInterface
              fun onDrawChanged(active: Boolean) {
                scope.launch {
                  isDrawActive = active
                }
              }

              @JavascriptInterface
              fun onSpeedChanged(ms: Long) {
                scope.launch {
                  autoPlayIntervalMs = ms
                }
              }
            }, "AndroidBridge")

            loadUrl("file:///android_asset/index.html")
            onRegisterWebView(this)
          }
        }
      )

      // Visual Timer Bar at Top of Viewport for Auto-Play (Cycles every 15 seconds)
      if (isAutoPlay) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .height(5.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0x33FFFFFF))
            .testTag("autoplay_timer_bar_container")
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(autoPlayProgress.value)
              .background(
                brush = Brush.horizontalGradient(
                  listOf(IndigoPrimary, CyanNeon, AccentEmerald)
                )
              )
          )
        }

        // Floating Auto-Play badge at top-right of presentation viewport
        Surface(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 10.dp, end = 12.dp)
            .testTag("autoplay_badge"),
          shape = RoundedCornerShape(99.dp),
          color = Color(0xEE0A0E1C),
          border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(CyanNeon)
            )
            Text(
              text = "Auto-Play: ${autoPlayIntervalMs / 1000}s",
              color = CyanNeon,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }

  // Teacher Lesson Notes & Engagement Modal
  if (showNotesDialog) {
    val notes = remember {
      listOf(
        LessonNote(
          title = "Slide 1: Cover & Global Reach",
          time = "3 mins",
          objective = "Hook students by connecting English to gaming, Discord, TikTok, and tech.",
          studentActivity = "💬 Pair Share: Have students turn to a partner and list 3 English words they use daily outside of school.",
          teacherTip = "Keep the energy high! Ask students how many of them watch YouTube or play online games in English."
        ),
        LessonNote(
          title = "Slide 2: Origins & French Twist (1066)",
          time = "5 mins",
          objective = "Contrast Anglo-Saxon everyday vocabulary with French courtly culinary terms.",
          studentActivity = "⚔️ Sorting Game: Ask why farmers said 'cow/calf/swine' while the nobility said 'beef/veal/pork'.",
          teacherTip = "Highlight that modern English is an ongoing mashup of Germanic grit and French elegance."
        ),
        LessonNote(
          title = "Slide 3: Evolution Timeline",
          time = "6 mins",
          objective = "Trace 1,500 years from Old English Beowulf chant to digital gaming slang.",
          studentActivity = "⏳ Letter Hunt: Point out runic Thorn (þ) and Ash (æ) and discuss how printing presses standardized spelling.",
          teacherTip = "Play the audio pronunciation of Old English to let students hear how foreign it originally sounded."
        ),
        LessonNote(
          title = "Slide 4: Why Everyone Speaks It Today",
          time = "4 mins",
          objective = "Understand the 3-to-1 ratio of non-native to native speakers and global Lingua Franca status.",
          studentActivity = "🌐 Discussion: Why did English dominate international aviation, coding languages, and scientific papers?",
          teacherTip = "Emphasize that international English belongs to everyone who uses it."
        ),
        LessonNote(
          title = "Slide 5: English is Weird!",
          time = "4 mins",
          objective = "Celebrate linguistic oddities: ghost words, shortest sentence, and 45-letter tongue-twisters.",
          studentActivity = "🧠 Challenge: Have 3 student volunteers compete to pronounce 'Pneumonoultramicroscopicsilicovolcanoconiosis'!",
          teacherTip = "Use the laser pointer or drawing tool to circle the Greek and Latin roots inside the long word."
        ),
        LessonNote(
          title = "Slide 6: Interactive Quiz",
          time = "3 mins",
          objective = "Formative check on letter frequency in English vocabulary.",
          studentActivity = "🔥 Quick Vote: Ask for hand raises for A, B, C, or D before pressing 'Reveal'!",
          teacherTip = "Option B ('E') will light up green with celebration confetti!"
        ),
        LessonNote(
          title = "Slide 7: Conquer The World",
          time = "3 mins",
          objective = "Empower students to embrace their accent and use English to share their original creations.",
          studentActivity = "🌍 Reflection: Write one thing you want to achieve, code, or share in English this year.",
          teacherTip = "Finish with the Celebrate confetti party button to leave students inspired!"
        )
      )
    }
    val currentNote = notes.getOrElse(currentSlideIndex) { notes[0] }

    AlertDialog(
      onDismissRequest = {
        showNotesDialog = false
        onEvalJs("window.deck.closeNotes();")
      },
      title = {
        Text(
          text = "📋 ${currentNote.title}",
          color = CyanNeon,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
          Surface(
            color = SpaceCard,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "🎯 LEARNING OBJECTIVE (${currentNote.time})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = currentNote.objective, fontSize = 11.sp, color = TextPrimary)
            }
          }

          Surface(
            color = SpaceCard,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "💡 ENGAGEMENT ACTIVITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentAmber
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = currentNote.studentActivity, fontSize = 11.sp, color = TextPrimary)
            }
          }

          Surface(
            color = SpaceCard,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "🗣️ PRESENTER TIP",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentEmerald
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = currentNote.teacherTip, fontSize = 11.sp, color = TextSecondary)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showNotesDialog = false
            onEvalJs("window.deck.closeNotes();")
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text("Done", fontSize = 11.sp)
        }
      },
      containerColor = SpaceSurface,
      shape = RoundedCornerShape(16.dp)
    )
  }

  // AI Tutor Assistant Modal
  if (showAiTutorDialog) {
    AiTutorDialog(
      currentSlideTitle = slides.getOrNull(currentSlideIndex)?.title ?: "English Lesson",
      onDismiss = { showAiTutorDialog = false }
    )
  }
}

/**
 * Left Dashboard component anchored on the left of the screen.
 * Provides lesson outline, live presenter controls, slide jumping,
 * and pedagogical tips for students and teachers.
 */
@Composable
fun LeftDashboard(
  slides: List<SlideInfo>,
  currentSlide: Int,
  visibleFragments: Int,
  totalFragments: Int,
  isAutoPlay: Boolean,
  autoPlayIntervalMs: Long,
  isLaserActive: Boolean,
  isDrawActive: Boolean,
  onSelectSlide: (Int) -> Unit,
  onNext: () -> Unit,
  onPrev: () -> Unit,
  onToggleAutoPlay: () -> Unit,
  onChangeAutoPlaySpeed: (Long) -> Unit,
  onToggleLaser: () -> Unit,
  onToggleDraw: () -> Unit,
  onClearDraw: () -> Unit,
  onPronounceWord: () -> Unit,
  onSendReaction: (String) -> Unit,
  onOpenNotes: () -> Unit,
  onOpenAiTutor: () -> Unit,
  onRevealQuiz: () -> Unit,
  onCelebrate: () -> Unit,
  onReset: () -> Unit,
  onToggleSound: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Surface(
    modifier = modifier
      .background(SpaceSurface)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          listOf(IndigoPrimary.copy(alpha = 0.4f), CyanNeon.copy(alpha = 0.2f))
        ),
        shape = RoundedCornerShape(0.dp)
      ),
    color = SpaceSurface
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 10.dp)
        .verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Header Brand
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 2.dp)
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(
              brush = Brush.linearGradient(listOf(IndigoPrimary, CyanBright)),
              shape = RoundedCornerShape(8.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "🇬🇧", fontSize = 14.sp)
        }
        Column {
          Text(
            text = "English Deck",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = TextPrimary
          )
          Text(
            text = "DASHBOARD",
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = CyanNeon,
            letterSpacing = 1.sp
          )
        }
      }

      // Progress bar and counter
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(SpaceCard, RoundedCornerShape(8.dp))
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Slide ${currentSlide + 1} / ${slides.size}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
          Text(
            text = "Frag $visibleFragments/$totalFragments",
            fontSize = 9.sp,
            color = TextSecondary
          )
        }

        LinearProgressIndicator(
          progress = { (currentSlide + 1).toFloat() / slides.size.toFloat() },
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape),
          color = CyanNeon,
          trackColor = Color(0xFF1E293B)
        )
      }

      // Primary Slide Navigation Buttons (Next / Prev)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        OutlinedButton(
          onClick = onPrev,
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .testTag("prev_slide_button"),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary
          ),
          border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
          Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = "Previous",
            modifier = Modifier.size(16.dp)
          )
          Text(text = "Prev", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onNext,
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .testTag("next_slide_button"),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
          ),
          border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                brush = Brush.horizontalGradient(listOf(IndigoVibrant, CyanBright)),
                shape = RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(text = "Next", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
              Spacer(modifier = Modifier.width(2.dp))
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Auto-Play Feature Card with Embedded Pace Selector
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isAutoPlay) CyanNeon.copy(alpha = 0.5f) else Color(0x226366F1))
      ) {
        Column(
          modifier = Modifier.padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Button(
            onClick = onToggleAutoPlay,
            modifier = Modifier
              .fillMaxWidth()
              .height(30.dp)
              .testTag("autoplay_button"),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isAutoPlay) Color(0xFF1E1B4B) else Color(0xFF1E293B)
            ),
            border = BorderStroke(
              width = 1.dp,
              color = if (isAutoPlay) CyanNeon else Color(0x336366F1)
            ),
            contentPadding = PaddingValues(horizontal = 4.dp)
          ) {
            Icon(
              imageVector = if (isAutoPlay) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isAutoPlay) "Pause Auto-Play" else "Start Auto-Play",
              tint = if (isAutoPlay) CyanNeon else TextSecondary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isAutoPlay) "Auto: ${autoPlayIntervalMs / 1000}s (ON)" else "Auto-Play: ${autoPlayIntervalMs / 1000}s",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isAutoPlay) CyanNeon else TextPrimary
            )
          }

          // Segmented Pace Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            listOf(10000L to "10s", 15000L to "15s", 30000L to "30s").forEach { (speedMs, label) ->
              val isSelected = autoPlayIntervalMs == speedMs
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .height(22.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { onChangeAutoPlaySpeed(speedMs) }
                  .testTag("speed_chip_${speedMs / 1000}"),
                color = if (isSelected) CyanNeon else Color(0xFF090D1A),
                border = BorderStroke(0.5.dp, if (isSelected) CyanNeon else Color(0x33FFFFFF))
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF04060E) else TextSecondary
                  )
                }
              }
            }
          }
        }
      }

      // Presenter Tools Section Label
      Text(
        text = "PRESENTER TOOLS",
        fontSize = 8.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextSubtle,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 2.dp)
      )

      // Laser, Draw, and Clear Tools with distinct identity
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        OutlinedButton(
          onClick = onToggleLaser,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("laser_button"),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isLaserActive) Color(0xFF881337) else Color(0xFF2A0A14),
            contentColor = Color(0xFFFF2A5F)
          ),
          border = BorderStroke(1.dp, if (isLaserActive) Color(0xFFFF2A5F) else Color(0x55FF2A5F))
        ) {
          Text(text = "🔴 Laser", fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onToggleDraw,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("draw_button"),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDrawActive) Color(0xFF0E7490) else Color(0xFF082F49),
            contentColor = CyanNeon
          ),
          border = BorderStroke(1.dp, if (isDrawActive) CyanNeon else Color(0x5522D3EE))
        ) {
          Text(text = "✏️ Draw", fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onClearDraw,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("clear_draw_button"),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF1E293B),
            contentColor = TextSecondary
          ),
          border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
          Text(text = "🧹 Clear", fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Lesson Notes & Voice Lab (Distinct Warm Amber & Royal Violet)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Button(
          onClick = onOpenNotes,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("lesson_notes_button"),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF451A03)),
          border = BorderStroke(1.dp, Color(0x88FBBF24))
        ) {
          Text(text = "📋 Notes", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
        }

        Button(
          onClick = onPronounceWord,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("pronounce_word_button"),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 2.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B0764)),
          border = BorderStroke(1.dp, Color(0x88C084FC))
        ) {
          Text(text = "🔊 Voice", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
        }
      }

      // Dedicated AI Tutor Feature Trigger
      Button(
        onClick = onOpenAiTutor,
        modifier = Modifier
          .fillMaxWidth()
          .height(30.dp)
          .testTag("ai_tutor_button"),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.8f))
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = "AI Tutor",
          tint = CyanNeon,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "🤖 AI Tutor Spark",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      }

      // Action Utilities: Emerald Quiz & Radiant Gold Party
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Button(
          onClick = onRevealQuiz,
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .testTag("reveal_quiz_button"),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF064E3B)
          ),
          border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f))
        ) {
          Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = "Reveal",
            tint = AccentEmerald,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = "Quiz", fontSize = 10.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onCelebrate,
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .testTag("celebrate_button"),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF451A03)
          ),
          border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
        ) {
          Icon(
            imageVector = Icons.Default.Celebration,
            contentDescription = "Celebrate",
            tint = AccentAmber,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = "Party", fontSize = 10.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
        }
      }

      // Live Student Reactions: Circular Emoji Bubbles
      Text(
        text = "REACTIONS",
        fontSize = 8.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextSubtle,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 2.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        listOf("🔥", "🤯", "👏", "💯", "⚡").forEach { emoji ->
          Surface(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .clickable { onSendReaction(emoji) }
              .testTag("reaction_$emoji"),
            color = Color(0x33FFFFFF),
            border = BorderStroke(1.dp, Color(0x22FFFFFF))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = emoji, fontSize = 13.sp)
            }
          }
        }
      }

      Text(
        text = "SLIDE OUTLINE",
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = TextSubtle,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
      )

      // Slide list items
      slides.forEach { slide ->
        val isActive = slide.id == currentSlide
        val itemBg = if (isActive) {
          Brush.horizontalGradient(
            listOf(IndigoPrimary.copy(alpha = 0.35f), CyanBright.copy(alpha = 0.15f))
          )
        } else {
          Brush.horizontalGradient(
            listOf(SpaceCard.copy(alpha = 0.6f), SpaceCard.copy(alpha = 0.6f))
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(itemBg)
            .border(
              width = 1.dp,
              color = if (isActive) CyanNeon else Color.Transparent,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectSlide(slide.id) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("nav_slide_${slide.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(text = slide.icon, fontSize = 13.sp)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${slide.id + 1}. ${slide.title}",
              fontSize = 10.sp,
              fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
              color = if (isActive) TextPrimary else TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Teacher Pedagogy Cue Box for active slide
      val activeSlideInfo = slides.getOrNull(currentSlide) ?: slides[0]
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(text = "💡", fontSize = 11.sp)
          Text(
            text = "LESSON CUE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = AccentAmber
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = activeSlideInfo.pedagogyTip,
          fontSize = 9.sp,
          color = TextSecondary,
          lineHeight = 12.sp
        )
      }

      // Deck Utilities: Sound & Restart
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        OutlinedButton(
          onClick = onToggleSound,
          modifier = Modifier
            .weight(1f)
            .height(28.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(SpaceCardBorder, SpaceCardBorder))
          )
        ) {
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Sound",
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = "Sound", fontSize = 9.sp, color = TextSecondary)
        }

        OutlinedButton(
          onClick = onReset,
          modifier = Modifier
            .weight(1f)
            .height(28.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 4.dp),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(SpaceCardBorder, SpaceCardBorder))
          )
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset",
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = "Reset", fontSize = 9.sp, color = TextSecondary)
        }
      }
    }
  }
}

@Composable
fun AiTutorDialog(
  currentSlideTitle: String,
  onDismiss: () -> Unit
) {
  var messages by remember {
    mutableStateOf(
      listOf(
        ChatMessage(
          sender = "Spark (AI Tutor)",
          text = "Hi! I'm Spark, your AI English Tutor 🤖. We're currently studying '$currentSlideTitle'. Ask me anything about English vocabulary, grammar, origins, or pronunciation!"
        )
      )
    )
  }
  var questionInput by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  fun sendQuestion(text: String) {
    if (text.isBlank() || isLoading) return
    val userMsg = ChatMessage("You", text)
    messages = messages + userMsg
    questionInput = ""
    isLoading = true

    scope.launch {
      val answer = AiTutorService.askTutor(text, currentSlideTitle)
      val tutorMsg = ChatMessage("Spark (AI Tutor)", answer)
      messages = messages + tutorMsg
      isLoading = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(
              brush = Brush.linearGradient(listOf(IndigoPrimary, CyanNeon)),
              shape = CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Tutor",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Column {
          Text(
            text = "AI English Tutor (Spark)",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "Topic: $currentSlideTitle",
            color = CyanNeon,
            fontSize = 10.sp
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Quick Prompt Suggestions Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("Explain slide", "Quiz me!", "Origins").forEach { suggestion ->
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { sendQuestion(suggestion) }
                .testTag("ai_suggestion_$suggestion"),
              color = Color(0xFF1E293B),
              border = BorderStroke(0.5.dp, CyanNeon.copy(alpha = 0.5f))
            ) {
              Text(
                text = suggestion,
                fontSize = 9.sp,
                color = CyanNeon,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Messages List
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF0A0E1A), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(messages) { msg ->
            val isUser = msg.sender == "You"
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
              Text(
                text = msg.sender,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) CyanBright else AccentAmber
              )
              Surface(
                color = if (isUser) Color(0xFF1E1B4B) else Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                  0.5.dp,
                  if (isUser) IndigoVibrant else Color(0x33FFFFFF)
                )
              ) {
                Text(
                  text = msg.text,
                  fontSize = 11.sp,
                  color = TextPrimary,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
          }
          if (isLoading) {
            item {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(4.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  color = CyanNeon,
                  strokeWidth = 1.5.dp
                )
                Text(
                  text = "Spark is thinking...",
                  fontSize = 10.sp,
                  color = TextSecondary
                )
              }
            }
          }
        }

        // Input Field and Send Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = questionInput,
            onValueChange = { questionInput = it },
            placeholder = { Text("Ask Spark anything...", fontSize = 11.sp, color = TextSubtle) },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("ai_tutor_input"),
            singleLine = true,
            colors = TextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF1E293B),
              unfocusedContainerColor = Color(0xFF0F172A),
              focusedIndicatorColor = CyanNeon,
              unfocusedIndicatorColor = Color(0x33FFFFFF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          Button(
            onClick = { sendQuestion(questionInput) },
            enabled = questionInput.isNotBlank() && !isLoading,
            modifier = Modifier
              .size(48.dp)
              .testTag("ai_tutor_send_button"),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = IndigoPrimary
            ),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Send",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
      ) {
        Text("Close", fontSize = 11.sp)
      }
    },
    containerColor = SpaceSurface,
    shape = RoundedCornerShape(16.dp)
  )
}

