package com.example.cashmanage.ui.screens


import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.AIChatViewModel
import kotlinx.coroutines.launch
import java.io.InputStream



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAdvisorScreen(
    onBack:()->Unit,
    viewModel:AIChatViewModel = viewModel()
){

    val context = LocalContext.current

    val state by viewModel.state.collectAsState()


    var input by remember {
        mutableStateOf("")
    }


    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }


    var isListening by remember {
        mutableStateOf(false)
    }


    val scope = rememberCoroutineScope()



    /*
        IMAGE PICKER
     */

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ){ uri ->

            uri?.let {


                try {

                    val stream:InputStream? =
                        context.contentResolver
                            .openInputStream(it)


                    selectedBitmap =
                        BitmapFactory
                            .decodeStream(stream)


                    stream?.close()


                }catch(e:Exception){


                }


            }

        }




    /*
        SPEECH
     */

    val speechRecognizer =
        remember {

            SpeechRecognizer
                .createSpeechRecognizer(
                    context
                )

        }



    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){

            granted ->

            if(granted){

                val intent =
                    Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    )


                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "id-ID"
                )


                speechRecognizer
                    .startListening(intent)


                isListening=true

            }

        }



    DisposableEffect(Unit){


        speechRecognizer
            .setRecognitionListener(

                object:
                    android.speech.RecognitionListener{


                    override fun onReadyForSpeech(
                        params:Bundle?
                    ){}


                    override fun onBeginningOfSpeech(){}


                    override fun onRmsChanged(
                        rmsdB:Float
                    ){}


                    override fun onBufferReceived(
                        buffer:ByteArray?
                    ){}


                    override fun onEndOfSpeech(){

                        isListening=false

                    }



                    override fun onError(
                        error:Int
                    ){

                        isListening=false

                    }



                    override fun onResults(
                        results:Bundle?
                    ){

                        val text =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )


                        if(!text.isNullOrEmpty()){

                            input=text[0]

                        }

                        isListening=false

                    }



                    override fun onPartialResults(
                        partialResults:Bundle?
                    ){}


                    override fun onEvent(
                        eventType:Int,
                        params:Bundle?
                    ){}


                }

            )



        onDispose {

            speechRecognizer.destroy()

        }

    }



    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "AI Financial Assistant"
                    )
                },


                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ){

                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )

                    }

                }

            )

        }

    ){ padding ->



        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)

        ){



            LazyColumn(

                modifier =
                    Modifier
                        .weight(1f)

            ){



                items(
                    state.messages
                ){ msg ->



                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp),


                        horizontalArrangement =
                        if(msg.isUser)
                            Arrangement.End
                        else
                            Arrangement.Start


                    ){



                        Card(

                            shape =
                            RoundedCornerShape(
                                16.dp
                            )

                        ){


                            Column(

                                modifier =
                                    Modifier
                                        .padding(12.dp)

                            ){



                                msg.bitmap?.let {


                                    Image(

                                        bitmap =
                                        it.asImageBitmap(),


                                        contentDescription =
                                        null,


                                        modifier =
                                            Modifier
                                                .size(150.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        10.dp
                                                    )
                                                )

                                    )

                                }



                                Text(
                                    msg.text
                                )

                            }


                        }



                    }


                }



                if(state.isLoading){


                    item {

                        Text(
                            "AI sedang menganalisa..."
                        )

                    }


                }



            }



            /*
                TRANSACTION PREVIEW
             */


            state.pendingTransaction?.let {


                Card(

                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical=8.dp)

                ){


                    Column(

                        modifier =
                            Modifier.padding(12.dp)

                    ){


                        Text(
                            "Konfirmasi transaksi",
                            style =
                            MaterialTheme
                                .typography
                                .titleMedium
                        )


                        Text(
                            "Jenis : ${it.type}"
                        )


                        Text(
                            "Kategori ID : ${it.categoryId}"
                        )


                        Text(
                            "Rekening ID : ${it.accountId}"
                        )


                        Text(
                            "Nominal : Rp ${it.amount}"
                        )


                        Text(
                            "Catatan : ${it.notes}"
                        )



                        Row{


                            Button(

                                onClick={
                                    viewModel
                                        .confirmTransaction()
                                }

                            ){

                                Text(
                                    "Simpan"
                                )

                            }



                            Spacer(
                                Modifier.width(8.dp)
                            )


                            OutlinedButton(

                                onClick = {
                                    viewModel
                                        .cancelTransaction()
                                }

                            ){

                                Text(
                                    "Batal"
                                )

                            }


                        }



                    }


                }


            }






            selectedBitmap?.let {


                Image(

                    bitmap =
                    it.asImageBitmap(),


                    contentDescription =
                    null,


                    modifier =
                        Modifier
                            .size(80.dp)

                )

            }






            Row(

                verticalAlignment =
                Alignment.CenterVertically

            ){



                IconButton(

                    onClick = {
                        imagePicker.launch(
                            "image/*"
                        )
                    }

                ){

                    Icon(
                        Icons.Default.Image,
                        null
                    )

                }




                IconButton(

                    onClick={


                        if(isListening){

                            speechRecognizer
                                .stopListening()

                            isListening=false


                        }else{


                            permissionLauncher
                                .launch(
                                    Manifest.permission.RECORD_AUDIO
                                )

                        }


                    }

                ){


                    Icon(
                        Icons.Default.Mic,
                        null
                    )


                }





                TextField(

                    value=input,


                    onValueChange={
                        input=it
                    },


                    modifier =
                        Modifier
                            .weight(1f),


                    placeholder = {

                        Text(
                            "Contoh: beli makan 25 ribu cash"
                        )

                    }


                )





                IconButton(


                    enabled =
                    input.isNotBlank()
                    ||
                    selectedBitmap!=null,


                    onClick={


                        val text=input

                        val img=selectedBitmap


                        input=""
                        selectedBitmap=null



                        viewModel
                            .sendMessage(
                                text,
                                img
                            )

                    }


                ){


                    Icon(
                        Icons.Default.Send,
                        null
                    )


                }



            }


        }


    }



}