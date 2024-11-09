package yos.music.player.ui.pages

import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.BitmapResolver
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultAlbum
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.models.ImageViewModel
import yos.music.player.ui.UI
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.effects.imageResolve
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper

@Composable
fun Home(
    navController: NavController,
    imageViewModel: ImageViewModel
) = Title(
    title = stringResource(id = R.string.page_home_title),
        rightIcon = CupertinoIcons.Default.PersonCropCircle,
        onRightIcon = {
            navController.toUI(UI.Settings.Main)
        },
        content = {
            item("RecommendCard") {
                RecommendCard(imageViewModel)
            }
        })

@Composable
fun RecommendCard(imageViewModel: ImageViewModel) {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    val showRecommend = remember(musicList/*,"MainActivity_showRecommend"*/) {
        derivedStateOf { musicList.isNotEmpty() }
    }

    if (showRecommend.value) {
        val randomMusicList = remember("RecommendCard_randomMusicList") {
            imageViewModel.recommendMusicList
        }

        YosWrapper {
            LaunchedEffect(showRecommend.value) {
                if (randomMusicList.value.isNotEmpty()) return@LaunchedEffect
                randomMusicList.value = musicList.shuffled().take(5)
            }
        }

        val pagerState = rememberPagerState(pageCount = { randomMusicList.value.size })

        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_recommend_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            val scope = rememberCoroutineScope()


            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(278.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 136.dp),
                key = { randomMusicList.value[it] },
                beyondViewportPageCount = 5
            ) { page ->
                val music = randomMusicList.value[page]
                RecommendCardItem(subTitle = stringResource(id = R.string.home_recommend_subtitle),
                    music = music,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            MediaController.prepare(
                                music,
                                randomMusicList.value
                            )
                        }
                    })
            }
        }
    }
}

@Composable
fun RecommendCardItem(subTitle: String, music: YosMediaItem, onClick: () -> Unit) =
    Column(Modifier.width(268.dp)) {

        val drawable = remember(music.thumb) {
            mutableStateOf<Drawable?>(null)
        }

        val context = LocalContext.current
        val imageLoader = ImageLoader(context)
        YosWrapper {
            LaunchedEffect(Unit) {
                if (music.thumb == null) return@LaunchedEffect

                delay(200)
                val request = ImageRequest.Builder(context)
                    .data(music.thumb)
                    .build()
                val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                    BitmapResolver.bitmapCompress(this, lowQuality = true)
                }

                if (thisBitmap != null) {
                    drawable.value = imageResolve(
                        thisBitmap
                    ).toDrawable(context.resources)
                    thisBitmap.recycle()
                }
                imageLoader.shutdown()
            }
        }

        Text(text = subTitle, fontSize = 15.sp, lineHeight = 15.sp, modifier = Modifier.alpha(0.6f))

        val shape = YosRoundedCornerShape(14.dp)
        // val cornerRadiusPx = 12.dp.toPx()

            val density = LocalDensity.current
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(354.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        clip = true
                        this.shape = shape
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            val outline = shape.createOutline(
                                Size(size.width, size.height),
                                LayoutDirection.Ltr,
                                density
                            )
                            drawOutline(
                                outline = outline,
                                color = Color.DarkGray.copy(alpha = 0.08f),
                                style = Stroke(width = 8f)
                            )
                            drawOutline(
                                outline = outline,
                                color = Color.DarkGray.copy(alpha = 0.4f),
                                style = Stroke(width = 8f),
                                blendMode = BlendMode.Overlay
                            )
                        }
                    }
                    .clickable(onClick = onClick)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(data = music.thumb)
                        .crossfade(true).error(R.drawable.placeholder_music_default_artwork)
                        .placeholder(R.drawable.placeholder_music_default_artwork)
                        .fallback(R.drawable.placeholder_music_default_artwork).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(268.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                    contentColor = Color.White
                ) {
                    YosWrapper {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(data = drawable.value).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                        )
                    }

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = music.title ?: defaultTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = music.artistsName ?: defaultArtistsName,
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .alpha(0.6f)
                                .padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = music.album ?: defaultAlbum,
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .alpha(0.6f)
                                .padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
    }

/*
fun handleImage(
    image: Bitmap
): Bitmap {
    var saturationBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
    saturationBitmap.applyCanvas {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(3f)
        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        drawBitmap(saturationBitmap, 0f, 0f, paint)
        //Color(0x40000000)
        drawColor((0x99000000).toInt(), PorterDuff.Mode.OVERLAY)
        drawColor((0x40000000).toInt())
    }
    saturationBitmap = saturationBitmap.scale(16, 16)
    //saturationBitmap = meshBitmap(saturationBitmap)
    saturationBitmap = Toolkit.blur(saturationBitmap, 25)
    return saturationBitmap
}*/
