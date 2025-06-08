package com.example.memekeyboard

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.memekeyboard.data.Meme
import java.io.File
import kotlin.coroutines.Continuation

class MemeThumbnailAdapter(
    private var memes: List<Meme>,
    private val onMemeClick: (Uri) -> Unit,
    private val onMemeLongClick: (Meme) -> Unit
) : RecyclerView.Adapter<MemeThumbnailAdapter.MemeVH>() {

    inner class MemeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val tvTags: TextView      = itemView.findViewById(R.id.tv_tags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemeVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meme_thumbnail, parent, false)
        return MemeVH(view)
    }

    override fun onBindViewHolder(holder: MemeVH, position: Int) {
        val meme = memes[position]
        Log.d("ADAPTER MEMES", "Binding meme at $position: ${meme.imagePath}")
        holder.ivThumbnail.load(File(meme.imagePath)) { crossfade(true) }

        holder.tvTags.text = meme.tags

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val file = File(meme.imagePath)

            if (!file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                Toast.makeText(context, "File is outside allowed directory", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            onMemeClick(uri)
        }


        holder.itemView.setOnLongClickListener {
            val context = it.context
            val popup = PopupMenu(context, holder.itemView)
            popup.menuInflater.inflate(R.menu.meme_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_copy_uri -> {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Meme URI", meme.imagePath)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "URI copied", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_delete -> {
                        onMemeLongClick(meme)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
            true // <- This tells Android that the long click was handled
        }


    }

    override fun getItemCount(): Int = memes.size

    fun submitList(newList: List<Meme>) {
        memes = newList
        notifyDataSetChanged()
    }

}
