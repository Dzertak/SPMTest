package com.dzertak.spmtest

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform