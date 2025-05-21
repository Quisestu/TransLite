package com.example.test0.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.test0.model.Language

@Composable
fun TranslationCard(
    sourceText: String,
    translatedText: String,
    sourceLanguage: Language,
    targetLanguage: Language,
    isTranslating: Boolean,
    onSourceTextChanged: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onClearText: () -> Unit,
    onCameraClicked: () -> Unit,
    onMicrophoneClicked: () -> Unit,
    onSpeakClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 源文本输入框
            OutlinedTextField(
                value = sourceText,
                onValueChange = onSourceTextChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "输入要翻译的文本") },
                trailingIcon = {
                    if (sourceText.isNotEmpty()) {
                        IconButton(onClick = onClearText) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除文本"
                            )
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 功能按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCameraClicked) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "拍照识别文字"
                    )
                }
                
                IconButton(onClick = onMicrophoneClicked) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "语音输入"
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(onClick = { onSourceTextChanged(sourceText) }) {
                    Text(text = "点击翻译")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 翻译结果
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = translatedText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "翻译结果") },
                    readOnly = true,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(translatedText))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制到剪贴板"
                                )
                            }
                            
                            IconButton(onClick = onSpeakClicked) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = "朗读翻译"
                                )
                            }
                        }
                    }
                )
                
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
} 