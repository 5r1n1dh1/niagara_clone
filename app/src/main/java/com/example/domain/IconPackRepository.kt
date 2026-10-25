package com.example.domain

import com.example.data.InstalledIconPackInfo

interface IconPackRepository {
    fun getInstalledIconPacks(): List<InstalledIconPackInfo>
}
