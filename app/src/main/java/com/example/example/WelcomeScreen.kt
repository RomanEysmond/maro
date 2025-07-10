package com.example.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen() {
    val backgroundImage: Painter = painterResource(id = R.drawable.fon)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = backgroundImage,
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // или FillBounds, FillWidth, FillHeight и т.д.
        )



        Text(
            text = "Maro",
            fontSize = 35.sp,
            color = Color(0xFF22272E),
            modifier = Modifier.align(alignment = Alignment.Center)
        )


    }



    @Composable
    fun GlideImageWithPreview(
        data: Any?,
        modifier: Modifier? = null,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.Fit
    ) {
        if (data == null)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = contentDescription,
                modifier = modifier ?: Modifier,
                alignment = Alignment.Center,
                contentScale = contentScale
            )
        else {
            com.skydoves.landscapist.glide.GlideImage(
                imageModel = data,
                contentDescription = contentDescription,
                modifier = Modifier
                    .width(220.dp)


            )
        }
    }
}