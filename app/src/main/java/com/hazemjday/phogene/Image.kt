package com.hazemjday.phogene

data class Image(val prompt: String, val negative: String, val width: Number ,val height: Number,val steps: Number, val guidance: Number, val sampler : String, val data : String)
