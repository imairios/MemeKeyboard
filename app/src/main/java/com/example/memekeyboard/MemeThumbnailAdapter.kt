package com.example.memekeyboard

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.memekeyboard.data.Meme

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
        holder.ivThumbnail.load(meme.imagePath) { crossfade(true) }
        holder.tvTags.text = meme.tags

        holder.itemView.setOnClickListener {
            val uri = Uri.parse(meme.imagePath)
            onMemeClick(uri)
        }
        holder.itemView.setOnLongClickListener {
            onMemeLongClick(meme)
            true
        }
    }

    override fun getItemCount(): Int = memes.size

    fun submitList(newList: List<Meme>) {
        memes = newList
        notifyDataSetChanged()
    }
}
