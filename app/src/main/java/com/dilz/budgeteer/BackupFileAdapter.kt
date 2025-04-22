package com.dilz.budgeteer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFileAdapter(
    private var files: List<File> = emptyList(),
    private val onFileSelected: (File) -> Unit
) : RecyclerView.Adapter<BackupFileAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileDate: TextView = itemView.findViewById(R.id.file_date)
        val fileSize: TextView = itemView.findViewById(R.id.file_size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_backup_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        
        // Parse the date from the filename (format: budgeteer_backup_yyyyMMdd_HHmmss.json)
        val datePattern = "\\d{8}_\\d{6}".toRegex()
        val dateMatch = datePattern.find(file.name)
        
        // Format the date if found
        val formattedDate = if (dateMatch != null) {
            try {
                val dateStr = dateMatch.value
                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).parse(dateStr)
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date ?: Date())
            } catch (e: Exception) {
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            }
        } else {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
        }
        
        // Format file size
        val fileSize = when {
            file.length() < 1024 -> "${file.length()} B"
            file.length() < 1024 * 1024 -> "${file.length() / 1024} KB"
            else -> "${file.length() / (1024 * 1024)} MB"
        }
        
        // Display file information
        holder.fileName.text = file.name.replace("budgeteer_backup_", "").replace(".json", "")
        holder.fileDate.text = formattedDate
        holder.fileSize.text = fileSize
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onFileSelected(file)
        }
    }

    override fun getItemCount() = files.size

    fun updateFiles(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }
} 