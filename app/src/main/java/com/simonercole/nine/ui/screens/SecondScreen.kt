package com.simonercole.nine.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.simonercole.nine.R
import com.simonercole.nine.ui.MediaPlayerChooser
import com.simonercole.nine.ui.model.Difficulty
import com.simonercole.nine.ui.model.NineGameViewModelClassic
import com.simonercole.nine.ui.model.NineGameViewModelFactoryClassic
import com.simonercole.nine.ui.model.NineGameViewModelFactoryGauntlet
import com.simonercole.nine.ui.model.NineGameViewModelGauntlet
import com.simonercole.nine.ui.theme.AppTheme
import com.simonercole.nine.ui.theme.fontFamily
import com.simonercole.nine.ui.theme.fontFamily2
import com.simonercole.nine.ui.theme.fontFamily3
import kotlinx.coroutines.launch


@SuppressLint("RememberReturnType")
@Composable
fun SecondScreen(difficulty: String, navController: NavHostController) {
    val context = LocalContext.current
    (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    if (difficulty != "None") {
        val viewModel : NineGameViewModelClassic  = viewModel(factory = NineGameViewModelFactoryClassic(context.applicationContext as Application))
        if (viewModel.sessionStarted.not()) {
            viewModel.setUpGame(difficulty)
        }
        SecondScreenPortrait(viewModel = viewModel, navController = navController)
    }
    else
    {
        val viewModel : NineGameViewModelGauntlet = viewModel(factory = NineGameViewModelFactoryGauntlet(context.applicationContext as Application))
        if (viewModel.sessionStarted.not()) {
            viewModel.setUpGame(difficulty)
        }
        GauntletScreen(viewModel, navController)
    }
}

@SuppressLint("UnrememberedMutableInteractionSource", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SecondScreenPortrait(viewModel: NineGameViewModelClassic, navController: NavHostController, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
    val focusArray by viewModel.focusArray.observeAsState()
    val liveInput by viewModel.liveInput.observeAsState()
    val showButton by viewModel.showConfirm.observeAsState()
    val userAttempts by viewModel.currentAttempts.observeAsState()
    val sequenceStatus by viewModel.sequenceStatus.observeAsState()
    val musicStarted by viewModel.musicStarted.observeAsState()
    val firstGuess by viewModel.firstGuess.observeAsState()
    val userInputs by viewModel.inputs.observeAsState()
    val gameWon by viewModel.gameWon.observeAsState()
    val gameLost by viewModel.gameLost.observeAsState()
    val showWinDialog = remember { mutableStateOf(false) }
    val showLossDialog = remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var changedMusic by remember { mutableStateOf(false) }
    val timerExpired by viewModel.timerExpired.observeAsState()
    val timerValue = viewModel._timerValue.observeAsState()
    val backRequest = remember{ mutableStateOf(false) }
    val refreshScreen = remember{ mutableStateOf(false) }
    val newBestTime by viewModel.newBestTime.observeAsState()
    val musicPausedByOnPause = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state = rememberLazyListState()

    MediaPlayerChooser.initMusic()
    var currentMusic = remember { mutableStateOf(MediaPlayerChooser.gameMusic) }.value

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver{source, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    currentMusic.pause()
                    viewModel.pause()
                    musicPausedByOnPause.value = true
                }
            else if (event==Lifecycle.Event.ON_RESUME) {
                 if (musicPausedByOnPause.value) {
                     currentMusic.start()
                     viewModel.startTimer()
                     musicPausedByOnPause.value = false
                 }
                }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (gameWon!!) {
        showWinDialog.value = true
    }

    if (gameLost!!) {
        showLossDialog.value = true
    }

    if (showWinDialog.value) {
        if (changedMusic.not()) {
            currentMusic.stop()
            currentMusic = MediaPlayerChooser.victoryMusic
            currentMusic.start()
            changedMusic = true
        }
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                viewModel.resetMusic(currentMusic)
                showWinDialog.value = !showWinDialog.value
                navController.navigate(Routes.NINE_START)
            },
            title = { Text(text = "Congratulations!", color = Color.Black, style = AppTheme.typography.body1) },
            text = {
                Text(
                    text = "You won!",
                    style = AppTheme.typography.body1,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Play again?",
                    style = AppTheme.typography.body1,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppTheme.dimens.medium1)
                )
                if (newBestTime!!) {
                    Text(
                        text = "New record in ${viewModel.difficulty} mode : ${viewModel.newTime.value}",
                        style = AppTheme.typography.body1,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = AppTheme.dimens.small2)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetMusic(currentMusic)
                        showWinDialog.value = !showWinDialog.value
                        navController.navigate(Routes.SECOND_SCREEN + "/${viewModel.difficulty}")
                    },
                ) {
                    androidx.compose.material3.Text("Play again", style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetMusic(currentMusic)
                    showWinDialog.value = !showWinDialog.value
                    navController.navigate(Routes.NINE_START)
                }) {
                    androidx.compose.material3.Text("Main menu", style = AppTheme.typography.body1)
                }
            })
    }

    if (showLossDialog.value) {
        if (changedMusic.not()) {
            currentMusic.stop()
            currentMusic = MediaPlayerChooser.lossMusic
            currentMusic.start()
            changedMusic = true
        }
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                viewModel.resetMusic(currentMusic)
                showLossDialog.value = !showLossDialog.value
                navController.navigate(Routes.NINE_START)
            },
            title = { Text(text = "Game Lost", color = Color.Black, style = AppTheme.typography.body1) },
            text = {
                Text(
                    text = if (timerExpired!!.value) "Timer finished" else "Attempts finished",
                    style = AppTheme.typography.body1,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Play again?",
                    style = AppTheme.typography.body1,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppTheme.dimens.medium1)
                )

                Text(
                    text = "Sequence was : ${String(viewModel.sequenceToGuess)}",
                    style = AppTheme.typography.body1,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppTheme.dimens.small2)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetMusic(currentMusic)
                        showLossDialog.value = !showLossDialog.value
                        navController.navigate(Routes.SECOND_SCREEN + "/${viewModel.difficulty}")
                    },
                ) {
                    androidx.compose.material3.Text(text = "Play again", style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetMusic(currentMusic)
                    showLossDialog.value = !showLossDialog.value
                    navController.navigate(Routes.NINE_START)
                }) {
                    androidx.compose.material3.Text("Main menu", style = AppTheme.typography.body1)
                }
            })
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetMusic(currentMusic) }
    }

    if (firstGuess == false && musicStarted == true && (!backRequest.value && !refreshScreen.value)) {
        currentMusic.start()
    }

    BackHandler(enabled = true, onBack = {
        if (musicStarted!!) {
            viewModel.pause()
            currentMusic.pause()
        }
        backRequest.value = true
    })

    if (refreshScreen.value && musicStarted!!) {
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                refreshScreen.value = false
                currentMusic.start()
                viewModel.startTimer()
            },
            title = {
                Text(
                    text = "Game is still on.\nYou still want to refresh ?\nActual game will count as lost",
                    style = AppTheme.typography.body1,
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        refreshScreen.value = false
                        viewModel.resetMusic(currentMusic)
                        viewModel.saveGameToDB()
                        navController.navigate(Routes.SECOND_SCREEN + "/${viewModel.difficulty}")
                    },
                ) {
                    androidx.compose.material3.Text("Refresh",style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    refreshScreen.value = false
                    viewModel.startTimer()
                    currentMusic.start()
                }) {
                    androidx.compose.material3.Text("Cancel",style = AppTheme.typography.body1)
                }
            })
    }
    else if(refreshScreen.value){
        navController.navigate(Routes.SECOND_SCREEN + "/${viewModel.difficulty}")
        refreshScreen.value = false
    }

    if (backRequest.value && musicStarted!!) {
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                backRequest.value = false
                viewModel.startTimer()
                currentMusic.start()
            },
            title = {
                Text(
                    text = "Game is still on.\nYou still want to quit ?\nActual game will count as lost.",
                    style = AppTheme.typography.body1,
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backRequest.value = false
                        viewModel.resetMusic(currentMusic)
                        viewModel.saveGameToDB()
                        navController.navigate(Routes.NINE_START)
                    },
                ) {
                    androidx.compose.material3.Text("Quit",style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    backRequest.value = false
                    viewModel.startTimer()
                    currentMusic.start()
                }) {
                    androidx.compose.material3.Text("Cancel",style = AppTheme.typography.body1)
                }
            })
    }else if(backRequest.value){
        navController.navigate(Routes.NINE_START)
        backRequest.value = false
    }

    if (isMuted) {
        currentMusic.pause()
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.gambacc),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

    }

    ConstraintLayout(Modifier.fillMaxSize()) {
        ConstraintLayoutMargins.SetConstraintMargins()
        val (firstRow, distanceRow, testBox, text, title, refreshButton, timer, backIcon, attempts, muteBtn, seqBox) = createRefs()
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(backIcon) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                }

                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted!!) {
                        viewModel.pause()
                        currentMusic.pause()
                    }
                    backRequest.value = true
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black
        )

        Icon(

            imageVector = Icons.Default.Refresh,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(refreshButton) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                }
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted!!) {
                        viewModel.pause()
                        currentMusic.pause()
                    }
                    refreshScreen.value = true
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black
        )

        Text(
            text = "Nine",
            style = AppTheme.typography.h1,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.smallMargin2)
                    start.linkTo(backIcon.end)
                    end.linkTo(parent.end, ConstraintLayoutMargins.largeMargin)
                }
        )

        Text(
            text = "Attempts : ${userAttempts}/${viewModel.attempts}",
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(attempts) {
                    top.linkTo(refreshButton.bottom, ConstraintLayoutMargins.smallMargin3)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin3)
                })

        Text(
            text = getTimerLabel(timerValue.value!!.value),
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(timer) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    end.linkTo(refreshButton.start, ConstraintLayoutMargins.smallMargin3)
                })

        Icon(

            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(muteBtn) {
                    top.linkTo(attempts.bottom, ConstraintLayoutMargins.smallMargin2)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                }
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted == true) {
                        if (!isMuted) isMuted = true
                        else {
                            isMuted = false
                            currentMusic.start()
                        }
                    }
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black)

        Row(
            modifier = Modifier
                .constrainAs(distanceRow) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.buttonHeight)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }

        ) {
        }

        Row(
            modifier = Modifier
                .constrainAs(firstRow) {
                    top.linkTo(distanceRow.bottom, ConstraintLayoutMargins.smallMargin2)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0..8) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimens.tileDimensions)
                        .border(
                            if (focusArray?.get(i) == 0 && sequenceStatus?.get(i) == 0) 2.dp else 1.dp,
                            color = if (focusArray?.get(i) == 0 && sequenceStatus?.get(i) == 0) Color.Red else Color.Black,
                        )
                        .background(if (liveInput!![i] == ' ') Color.White else Color(0xffA47449))
                        .clickable {
                            if (sequenceStatus?.get(i) == 0) {
                                viewModel.updateFocusByTouch(i)
                                viewModel.deleteChar(i)
                            } else viewModel.updateFocusByTouch(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    liveInput?.get(i)?.let {
                        Text(
                            text = it.toString(),
                            style = AppTheme.typography.h6,
                            color = Color.White
                        )
                    }
                }
            }

        }

        Text(
            text = "Past guesses",
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(text) {
                    top.linkTo(firstRow.bottom, ConstraintLayoutMargins.mediumMargin1)
                    start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                }
        )

        Box(modifier = Modifier
            .constrainAs(seqBox) {
                top.linkTo(text.bottom, ConstraintLayoutMargins.smallMargin3)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(testBox.top, ConstraintLayoutMargins.mediumMargin1)
            }
            .background(Color.White, RoundedCornerShape(AppTheme.dimens.small1))
            .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
            .border(2.dp, Color.Black, RoundedCornerShape(AppTheme.dimens.small1))
            .fillMaxHeight(0.3f)
            .fillMaxWidth(0.95f)
        ) {
            LazyColumn(modifier = Modifier.padding(AppTheme.dimens.small1), state = state) {

                coroutineScope.launch {
                    if (userInputs!!.size > 2) {
                        state.animateScrollToItem(userInputs!!.size - 1)
                    }
                }

                item {
                    if (userInputs!!.isEmpty().not()) {
                        userInputs!!.forEach {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                for (i in 0..8) {
                                    Box(
                                        modifier = Modifier
                                            .size(AppTheme.dimens.smallTileDimensions)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        it[i]?.first.toString().let { it1 ->
                                            Text(
                                                text = it1,
                                                style = AppTheme.typography.h6,
                                                color =
                                                if (it[i]?.first.toString() == "?") Color.Black
                                                else {
                                                    when (it[i]?.first.toString()) {
                                                        "0" -> Color.Green
                                                        "1" -> Color(0xff014462)
                                                        "2" -> Color(0xffadd8e6)
                                                        "3" -> Color(0xffFFA500)
                                                        else -> Color.Red
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                for (i in 0..8) {
                                    Box(
                                        modifier = Modifier
                                            .size(AppTheme.dimens.smallTileDimensions)//35.dp
                                            .border(
                                                1.dp,
                                                color = Color.Black,
                                            )
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = it[i]?.second.toString(),
                                            style = AppTheme.typography.h6,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(AppTheme.dimens.medium1))
                        }

                    }
                }
            }
        }



        Box(modifier = Modifier
            .constrainAs(testBox) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
        ) {


            ConstraintLayout {
                val (inputTop, inputBottom, confirm) = createRefs()

                if (showButton == true) {
                    Card(onClick = {
                        viewModel.makeGuess()
                    },
                        modifier = Modifier
                            .constrainAs(confirm) {
                                top.linkTo(testBox.bottom, ConstraintLayoutMargins.mediumMargin3)
                                start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                                end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                                bottom.linkTo(parent.bottom)
                            }
                            .size(AppTheme.dimens.logoSize, AppTheme.dimens.medium3),
                        backgroundColor = Color(0xfff8f1e7),
                        shape = RoundedCornerShape(AppTheme.dimens.small3),
                        elevation = AppTheme.dimens.small1
                    )

                    {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {
                            Text(
                                style = AppTheme.typography.h6,
                                text = "Confirm",
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .constrainAs(inputTop) {
                            top.linkTo(testBox.bottom, ConstraintLayoutMargins.mediumMargin2)
                            start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                            end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                        }
                        .fillMaxWidth()
                        .padding(start = AppTheme.dimens.small1, end = AppTheme.dimens.small2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    for (i in 0..4) {
                        if (liveInput?.contains(viewModel.startingKeyboard[i])
                                ?.not() == true && viewModel.guessedChars.contains(viewModel.startingKeyboard[i])
                                .not()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(AppTheme.dimens.medium2)
                                    .clickable {
                                        focusArray
                                            ?.indexOf(0)
                                            ?.let {
                                                viewModel.updateInput(
                                                    it,
                                                    viewModel.startingKeyboard[i]
                                                )
                                            }
                                    }
                                    .background(
                                        Color(0xffA47449),
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    )
                                    .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                    .border(
                                        2.dp,
                                        Color.Black,
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    ),
                                contentAlignment = Alignment.Center,
                            )
                            {
                                Text(
                                    text = viewModel.startingKeyboard[i].toString(),
                                    style = AppTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .size(AppTheme.dimens.medium2)
                                .background(
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                )
                                .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .border(
                                    2.dp,
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                ))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .constrainAs(inputBottom) {
                            top.linkTo(inputTop.bottom, ConstraintLayoutMargins.mediumMargin2)
                            start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin3)
                            end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin2)
                        }
                        .fillMaxWidth()
                        .padding(start = AppTheme.dimens.medium2, end = AppTheme.dimens.medium2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    for (i in 5..8) {
                        if (liveInput?.contains(viewModel.startingKeyboard[i])
                                ?.not() == true && viewModel.guessedChars.contains(viewModel.startingKeyboard[i])
                                .not()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(AppTheme.dimens.medium2)
                                    .clickable {
                                        focusArray
                                            ?.indexOf(0)
                                            ?.let {
                                                viewModel.updateInput(
                                                    it,
                                                    viewModel.startingKeyboard[i]
                                                )
                                            }
                                    }
                                    .background(
                                        Color(0xffA47449),
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    )
                                    .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                    .border(
                                        2.dp,
                                        Color.Black,
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    ),
                                contentAlignment = Alignment.Center,
                            )
                            {
                                Text(
                                    text = viewModel.startingKeyboard[i].toString(),
                                    style = AppTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .size(AppTheme.dimens.medium2)
                                .background(
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                )
                                .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .border(
                                    2.dp,
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                ))

                        }

                    }
                }

            }
        }
    }
}



@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
fun GauntletScreen(viewModel: NineGameViewModelGauntlet, navController: NavHostController, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
    val focusArray by viewModel.focusArray.observeAsState()
    val liveInput by viewModel.liveInput.observeAsState()
    val showButton by viewModel.showConfirm.observeAsState()
    val sequenceStatus by viewModel.sequenceStatus.observeAsState()
    val musicStarted by viewModel.musicStarted.observeAsState()
    val firstGuess by viewModel.firstGuess.observeAsState()
    val userInputs by viewModel.inputs.observeAsState()
    val score by viewModel.score.observeAsState()
    val showEndedGameDialog = remember{mutableStateOf(false)}
    var isMuted by remember { mutableStateOf(false) }
    var changedMusic by remember{ mutableStateOf(false) }
    val sessionEnded by viewModel.sessionEnded.observeAsState()
    val backRequest = remember{ mutableStateOf(false) }
    val refreshScreen = remember{ mutableStateOf(false) }
    val timerValue = viewModel._timerValue.observeAsState()
    val bestScoreChanged by viewModel.bestScoreChanged.observeAsState()
    val musicPausedByOnPause = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state = rememberLazyListState()

    MediaPlayerChooser.initMusic()
    var currentMusic by remember{ mutableStateOf(MediaPlayerChooser.gameMusic) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver{source, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                currentMusic.pause()
                viewModel.pause()
                musicPausedByOnPause.value = true
            }
            else if (event==Lifecycle.Event.ON_RESUME) {
                if (musicPausedByOnPause.value) {
                    currentMusic.start()
                    viewModel.startTimer()
                    musicPausedByOnPause.value = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (sessionEnded!!.value) {
        showEndedGameDialog.value = true
    }


    if (showEndedGameDialog.value) {
        if (changedMusic.not()) {
            currentMusic.stop()
            currentMusic = MediaPlayerChooser.victoryMusic
            currentMusic.start()
            changedMusic = true
        }
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
            },
            title = { Text(text = "Game ended", color = Color.Black) },
            text = {
                if (bestScoreChanged!!) {
                    Text(
                        text = "New record : " + score!!.intValue,
                        style = AppTheme.typography.body1,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = AppTheme.dimens.small2)
                    )
                }
                else {
                    Text(
                        text = "Your score :  " + score!!.intValue,
                        style = AppTheme.typography.body1,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Play again?",
                        style = AppTheme.typography.body1,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = AppTheme.dimens.medium1)
                    )
                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetMusic(currentMusic)
                        showEndedGameDialog.value = !showEndedGameDialog.value
                        navController.navigate(Routes.SECOND_SCREEN + "/${Difficulty.None}")
                    },
                ) {
                    Text(text = "Play again",style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetMusic(currentMusic)
                    showEndedGameDialog.value = !showEndedGameDialog.value
                    navController.navigate(Routes.NINE_START)
                }) {
                    androidx.compose.material3.Text("Main menu",style = AppTheme.typography.body1)
                }
            })
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetMusic(currentMusic) }
    }

    if (firstGuess == false && musicStarted == true && (!refreshScreen.value && !backRequest.value)) {
        currentMusic.start()
    }

    BackHandler(enabled = true, onBack = {
        if (musicStarted!!) {
            viewModel.pause()
            currentMusic.pause()
        }
        backRequest.value = true
    })

    if (refreshScreen.value && musicStarted!!) {
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                refreshScreen.value = false
                viewModel.startTimer()
                currentMusic.start()
            },
            title = {
                Text(
                    text = "Game is still on.\nYou still want to refresh ?",
                    style = AppTheme.typography.body1,
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        refreshScreen.value = false
                        viewModel.resetMusic(currentMusic)
                        navController.navigate(Routes.SECOND_SCREEN + "/${Difficulty.None}")
                    },
                ) {
                   Text("Refresh", style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    refreshScreen.value = false
                    viewModel.startTimer()
                    currentMusic.start()
                }) {
                    Text("Cancel", style = AppTheme.typography.body1)
                }
            })
    }
    else if(refreshScreen.value){
        navController.navigate(Routes.SECOND_SCREEN + "/${Difficulty.None}")
        refreshScreen.value = false
    }

    if (backRequest.value && musicStarted!!) {
        AlertDialog(
            backgroundColor = Color(0xfffff8dc),
            onDismissRequest = {
                backRequest.value = false
                currentMusic.start()
                viewModel.startTimer()
            },
            title = {
                Text(
                    text = "Game is still on.\nYou still want to quit ?",
                    style = AppTheme.typography.body1,
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backRequest.value = false
                        viewModel.resetMusic(currentMusic)
                        navController.navigate(Routes.NINE_START)
                    },
                ) {
                    androidx.compose.material3.Text("Quit", style = AppTheme.typography.body1)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    backRequest.value = false
                    currentMusic.start()
                    viewModel.startTimer()
                }) {
                    androidx.compose.material3.Text("Cancel", style = AppTheme.typography.body1)
                }
            })
    }else if(backRequest.value){
        navController.navigate(Routes.NINE_START)
        backRequest.value = false
    }

    if (isMuted) {
        currentMusic.pause()
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.gambacc),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

    }

    ConstraintLayout(Modifier.fillMaxSize()) {
        ConstraintLayoutMargins.SetConstraintMargins()
        val (firstRow, distanceRow, testBox, text, title, refreshButton, timer, backIcon, attempts, muteBtn, seqBox) = createRefs()
        Icon(

            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(backIcon) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                }

                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted!!) {
                        viewModel.pause()
                        currentMusic.pause()
                    }
                    backRequest.value = true
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black
        )

        Icon(

            imageVector = Icons.Default.Refresh,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(refreshButton) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                }
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted!!) {
                        viewModel.pause()
                        currentMusic.pause()
                    }
                    refreshScreen.value = true
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black
        )

        Text(
            text = "Nine",
            style = AppTheme.typography.h1,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.smallMargin2)
                    start.linkTo(backIcon.end)
                    end.linkTo(parent.end, ConstraintLayoutMargins.largeMargin)
                }
        )

        Text(
            text = "Score : ${score!!.intValue}",
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(attempts) {
                    top.linkTo(refreshButton.bottom, ConstraintLayoutMargins.smallMargin3)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin3)
                })

        Text(
            text = getTimerLabel(timerValue.value!!.value) ,
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(timer) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.mediumMargin1)
                    end.linkTo(refreshButton.start, ConstraintLayoutMargins.smallMargin3)
                })

        Icon(

            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "info",
            modifier = Modifier
                .constrainAs(muteBtn) {
                    top.linkTo(attempts.bottom, ConstraintLayoutMargins.smallMargin2)
                    end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                }
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                {
                    if (musicStarted == true) {
                        if (!isMuted) isMuted = true
                        else {
                            isMuted = false
                            currentMusic.start()
                        }
                    }
                }
                .size(AppTheme.dimens.medium1),
            tint = Color.Black)

        Row(
            modifier = Modifier
                .constrainAs(distanceRow) {
                    top.linkTo(parent.top, ConstraintLayoutMargins.buttonHeight)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }

        ) {
        }

        Row(
            modifier = Modifier
                .constrainAs(firstRow) {
                    top.linkTo(distanceRow.bottom, ConstraintLayoutMargins.smallMargin2)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0..8) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimens.tileDimensions)
                        .border(
                            if (focusArray?.get(i) == 0 && sequenceStatus?.get(i) == 0) 2.dp else 1.dp,
                            color = if (focusArray?.get(i) == 0 && sequenceStatus?.get(i) == 0) Color.Red else Color.Black,
                        )
                        .background(if (liveInput!![i] == ' ') Color.White else Color(0xffA47449))
                        .clickable {
                            if (sequenceStatus?.get(i) == 0) {
                                viewModel.updateFocusByTouch(i)
                                viewModel.deleteChar(i)
                            } else viewModel.updateFocusByTouch(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    liveInput?.get(i)?.let {
                        Text(
                            text = it.toString(),
                            style = AppTheme.typography.h6,
                            color = Color.White
                        )
                    }
                }
            }

        }

        Text(
            text = "Past guesses",
            style = AppTheme.typography.h6,
            modifier = Modifier
                .constrainAs(text) {
                    top.linkTo(firstRow.bottom, ConstraintLayoutMargins.mediumMargin1)
                    start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                }
        )

        Box(modifier = Modifier
            .constrainAs(seqBox) {
                top.linkTo(text.bottom, ConstraintLayoutMargins.smallMargin3)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(testBox.top, ConstraintLayoutMargins.mediumMargin1)
            }
            .background(Color.White, RoundedCornerShape(AppTheme.dimens.small1))
            .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
            .border(2.dp, Color.Black, RoundedCornerShape(AppTheme.dimens.small1))
            .fillMaxHeight(0.3f)
            .fillMaxWidth(0.95f)
        ) {
            LazyColumn(modifier = Modifier.padding(AppTheme.dimens.small1), state = state) {
                coroutineScope.launch {
                    if (userInputs!!.size > 2) {
                        state.animateScrollToItem(userInputs!!.size - 1)
                    }
                }
                item {
                    if (userInputs!!.isEmpty().not()) {
                        userInputs!!.forEach {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ){
                                for (i in 0..8) {
                                    Box(
                                        modifier = Modifier
                                            .size(AppTheme.dimens.smallTileDimensions)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = it[i]?.first.toString(),
                                            style = AppTheme.typography.h6,
                                            color = when(it[i]?.first.toString()) {
                                                "0" -> Color.Green
                                                "1" -> Color(0xff014462)
                                                "2" -> Color(0xffadd8e6)
                                                "3" -> Color(0xffFFA500)
                                                else -> Color.Red
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ){
                                for (i in 0..8) {
                                    Box(
                                        modifier = Modifier
                                            .size(AppTheme.dimens.smallTileDimensions)
                                            .border(
                                                1.dp,
                                                color = Color.Black,
                                            )
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = it[i]?.second.toString(),
                                            style = AppTheme.typography.h6,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(AppTheme.dimens.medium1))
                        }

                    }
                }
            }
        }



        Box(modifier = Modifier
            .constrainAs(testBox) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
        ) {


            ConstraintLayout {
                val (inputTop, inputBottom, confirm) = createRefs()

                if (showButton == true) {
                    Card(onClick = {
                        viewModel.makeGuess()
                    },
                        modifier = Modifier
                            .constrainAs(confirm) {
                                top.linkTo(testBox.bottom, ConstraintLayoutMargins.mediumMargin3)
                                start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                                end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                                bottom.linkTo(parent.bottom)
                            }
                            .size(AppTheme.dimens.logoSize, AppTheme.dimens.medium3),
                        backgroundColor = Color(0xfff8f1e7),
                        shape = RoundedCornerShape(AppTheme.dimens.small3),
                        elevation = AppTheme.dimens.small1
                    )

                    {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {
                            Text(
                                style = AppTheme.typography.h6,
                                text = "Confirm",
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .constrainAs(inputTop) {
                            top.linkTo(testBox.bottom, ConstraintLayoutMargins.mediumMargin2)
                            start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin1)
                            end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin1)
                        }
                        .fillMaxWidth()
                        .padding(start = AppTheme.dimens.small1, end = AppTheme.dimens.small2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    for (i in 0..4) {
                        if (liveInput?.contains(viewModel.startingKeyboard[i])
                                ?.not() == true && viewModel.guessedChars.contains(viewModel.startingKeyboard[i])
                                .not()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(AppTheme.dimens.medium2)
                                    .clickable {
                                        focusArray
                                            ?.indexOf(0)
                                            ?.let {
                                                viewModel.updateInput(
                                                    it,
                                                    viewModel.startingKeyboard[i]
                                                )
                                            }
                                    }
                                    .background(
                                        Color(0xffA47449),
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    )
                                    .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                    .border(
                                        2.dp,
                                        Color.Black,
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    ),
                                contentAlignment = Alignment.Center,
                            )
                            {
                                Text(
                                    text = viewModel.startingKeyboard[i].toString(),
                                    style = AppTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .size(AppTheme.dimens.medium2)
                                .background(Color.Transparent, shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .border(
                                    2.dp,
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                ))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .constrainAs(inputBottom) {
                            top.linkTo(inputTop.bottom, ConstraintLayoutMargins.mediumMargin2)
                            start.linkTo(parent.start, ConstraintLayoutMargins.smallMargin3)
                            end.linkTo(parent.end, ConstraintLayoutMargins.smallMargin2)
                        }
                        .fillMaxWidth()
                        .padding(start = AppTheme.dimens.medium2, end = AppTheme.dimens.medium2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    for (i in 5..8) {
                        if (liveInput?.contains(viewModel.startingKeyboard[i])
                                ?.not() == true && viewModel.guessedChars.contains(viewModel.startingKeyboard[i])
                                .not()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(AppTheme.dimens.medium2)
                                    .clickable {
                                        focusArray
                                            ?.indexOf(0)
                                            ?.let {
                                                viewModel.updateInput(
                                                    it,
                                                    viewModel.startingKeyboard[i]
                                                )
                                            }
                                    }
                                    .background(
                                        Color(0xffA47449),
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    )
                                    .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                    .border(
                                        2.dp,
                                        Color.Black,
                                        shape = RoundedCornerShape(AppTheme.dimens.small1)
                                    ),
                                contentAlignment = Alignment.Center,
                            )
                            {
                                Text(
                                    text = viewModel.startingKeyboard[i].toString(),
                                    style = AppTheme.typography.h6,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier
                                .size(AppTheme.dimens.medium2)
                                .background(Color.Transparent, shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .clip(shape = RoundedCornerShape(AppTheme.dimens.small1))
                                .border(
                                    2.dp,
                                    Color.Transparent,
                                    shape = RoundedCornerShape(AppTheme.dimens.small1)
                                ))

                        }

                    }
                }

            }
        }
    }
}

fun getTimerLabel(value: Int): String {
    return "${padding(value / 60)} : ${padding(value % 60)}"
}

fun padding(value: Int) = if (value < 10) ("0$value") else "" + value