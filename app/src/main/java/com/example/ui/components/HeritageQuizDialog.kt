package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.HeritageQuiz
import com.example.data.remote.QuizQuestion

@Composable
fun HeritageQuizDialog(
    quiz: HeritageQuiz,
    onDismiss: () -> Unit,
    onQuizComplete: ((score: Int, total: Int) -> Unit)? = null
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    val currentQuestion = quiz.questions[currentQuestionIndex]

    // Trigger onQuizComplete when showResults transitions to true
    LaunchedEffect(showResults) {
        if (showResults) {
            onQuizComplete?.invoke(score, quiz.questions.size)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Heritage Quiz",
                            color = Color(0xFF9AF04D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = quiz.siteName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!showResults) {
                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Question ${currentQuestionIndex + 1}/${quiz.questions.size}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Score: $score",
                            color = Color(0xFF9AF04D),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / quiz.questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF9AF04D),
                        trackColor = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question
                    Text(
                        text = currentQuestion.question,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isCorrect = index == currentQuestion.correctAnswerIndex
                        val isSelected = index == selectedOptionIndex
                        
                        val backgroundColor = when {
                            isAnswered && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            isAnswered && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                            isSelected -> Color(0xFF9AF04D).copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }

                        val borderColor = when {
                            isAnswered && isCorrect -> Color(0xFF4CAF50)
                            isAnswered && isSelected && !isCorrect -> Color(0xFFF44336)
                            isSelected -> Color(0xFF9AF04D)
                            else -> Color.Gray.copy(alpha = 0.3f)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(backgroundColor)
                                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                .clickable(enabled = !isAnswered) {
                                    selectedOptionIndex = index
                                }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${'A' + index}.",
                                    color = if (isSelected || (isAnswered && isCorrect)) Color(0xFF9AF04D) else Color.Gray,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = option,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAnswered && isCorrect) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Explanation or Action
                    if (isAnswered) {
                        Text(
                            text = "💡 ${currentQuestion.explanation}",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (currentQuestionIndex < quiz.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                    isAnswered = false
                                } else {
                                    showResults = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (currentQuestionIndex < quiz.questions.size - 1) "Next Question" else "See Results",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedOptionIndex != null) {
                                    isAnswered = true
                                    if (selectedOptionIndex == currentQuestion.correctAnswerIndex) {
                                        score++
                                    }
                                }
                            },
                            enabled = selectedOptionIndex != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9AF04D),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Submit Answer",
                                color = if (selectedOptionIndex != null) Color.Black else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Results View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quiz Completed!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9AF04D).copy(alpha = 0.1f))
                                .border(4.dp, Color(0xFF9AF04D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$score/${quiz.questions.size}",
                                    color = Color(0xFF9AF04D),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(text = "Correct", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val feedback = when {
                            score == quiz.questions.size -> "Heritage Master! 🏆 You know your history perfectly."
                            score >= quiz.questions.size / 2 -> "Well done! 🌟 You're becoming a true Heritage Explorer."
                            else -> "Keep exploring! 🗺️ Every site has a story to tell."
                        }
                        
                        Text(
                            text = feedback,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9AF04D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Close", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
