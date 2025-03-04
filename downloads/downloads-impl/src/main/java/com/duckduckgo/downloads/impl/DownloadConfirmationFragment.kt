/*
 * Copyright (c) 2019 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.downloads.impl

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.duckduckgo.anvil.annotations.InjectWith
import com.duckduckgo.di.scopes.FragmentScope
import com.duckduckgo.downloads.api.DownloadConfirmationDialogListener
import com.duckduckgo.downloads.api.FileDownloader.PendingFileDownload
import com.duckduckgo.downloads.impl.RealDownloadConfirmation.Companion.PENDING_DOWNLOAD_BUNDLE_KEY
import com.duckduckgo.downloads.impl.databinding.DownloadConfirmationBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import java.io.File
import javax.inject.Inject
import logcat.logcat

@InjectWith(FragmentScope::class)
class DownloadConfirmationFragment : BottomSheetDialogFragment() {

    val listener: DownloadConfirmationDialogListener
        get() {
            return if (parentFragment != null) {
                parentFragment as DownloadConfirmationDialogListener
            } else {
                activity as DownloadConfirmationDialogListener
            }
        }

    @Inject
    lateinit var filenameExtractor: FilenameExtractor

    private var file: File? = null

    private val pendingDownload: PendingFileDownload by lazy {
        requireArguments()[PENDING_DOWNLOAD_BUNDLE_KEY] as PendingFileDownload
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = DownloadConfirmationBinding.inflate(inflater, container, false)
        setupDownload()
        setupViews(binding)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.apply {
            isFitToContents = true
            peekHeight = resources.displayMetrics.heightPixels / 2
        }
        return dialog
    }

    private fun setupDownload() {
        file = if (!pendingDownload.isDataUrl) {
            when (val filenameExtraction = filenameExtractor.extract(pendingDownload)) {
                is FilenameExtractor.FilenameExtractionResult.Guess -> null
                is FilenameExtractor.FilenameExtractionResult.Extracted -> File(pendingDownload.directory, filenameExtraction.filename)
            }
        } else {
            null
        }
    }

    private fun setupViews(binding: DownloadConfirmationBinding) {
        val fileName = file?.name ?: ""
        binding.downloadMessage.text = fileName
        binding.continueDownload.setOnClickListener {
            listener.continueDownload(pendingDownload)
            dismiss()
        }
        binding.cancel.setOnClickListener {
            logcat { "Cancelled download for url ${pendingDownload.url}" }
            listener.cancelDownload()
            dismiss()
        }
    }
}
