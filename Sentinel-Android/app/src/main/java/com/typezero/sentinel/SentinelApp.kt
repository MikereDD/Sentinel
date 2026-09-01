package com.typezero.sentinel

import android.app.Application
import com.typezero.sentinel.data.db.SentinelDatabase
import com.typezero.sentinel.data.repository.SentinelRepository
import com.typezero.sentinel.scan.NetworkScanner

/** Lightweight manual dependency container (no Hilt needed for v1.0). */
class SentinelApp : Application() {
    lateinit var repository: SentinelRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = SentinelDatabase.get(this)
        val scanner = NetworkScanner(this)
        repository = SentinelRepository(db, scanner)
    }
}
