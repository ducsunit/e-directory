package com.ducsunit.appenglish

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import data_class.SaveDetailWord
import data_class.SaveRecentWord
import database.DatabaseHelper
import java.io.IOException

class MainRecentWord(private var context: Context, var wordList: List<SaveRecentWord>) :
    ArrayAdapter<SaveRecentWord>(context, R.layout.activity_recent_word) {
    private lateinit var adt: MainDetailWord // Adapter cho ListView
    override fun getCount(): Int {
        return wordList.size
    }


    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = LayoutInflater.from(context).inflate(R.layout.custom_recent_word, parent, false)

        val wordC = rowView.findViewById<TextView>(R.id.txtWord)
        val ipaC = rowView.findViewById<TextView>(R.id.txtIPA)
        val imgC = rowView.findViewById<ImageView>(R.id.imgSound)
        val imgDC = rowView.findViewById<ImageView>(R.id.imgDelete)
        val typeC = rowView.findViewById<TextView>(R.id.txtType)
        val defiC = rowView.findViewById<TextView>(R.id.txtDefinition)
        val imgH = rowView.findViewById<ImageView>(R.id.imgHobby)
        wordC.text = wordList[position].word
        ipaC.text = wordList[position].ipa

        imgC.setImageResource(wordList[position].img)
        imgH.setImageResource(wordList[position].imgH)
        imgDC.setImageResource(wordList[position].imgDelete)

        typeC.text = wordList[position].type
        defiC.text = wordList[position].defi

        imgC.setOnClickListener {
            playSound(wordList[position].word)
        }

        imgH.setOnClickListener{
            val dialog = AlertDialog.Builder(context)
            dialog.apply {
                setTitle("Thêm từ yêu thích")
                setPositiveButton("Có"){ dialogInterface: DialogInterface, i: Int ->
                    hobbyWord(wordList[position].word)
                }
                setNegativeButton("Không"){ dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss()
                }
            }
            dialog.show()
        }

        imgDC.setOnClickListener {
            val dialog = AlertDialog.Builder(context)
            dialog.apply {
                setTitle("Thông báo")
                setMessage("Bạn chắc chắn muốn xoá !")
                setPositiveButton("Có"){ dialogInterface: DialogInterface, i: Int ->
                    deleteItem(wordList[position].word)
                }
                setNegativeButton("Không"){ dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss()
                }
            }
            dialog.show()
        }
        // Đảm bảo không lan sự kiện click từ toàn bộ item
        rowView.setOnClickListener {
            // Không làm gì khi click vào item chính
        }
        return rowView
    }

    private  fun playSound(word: String) {
        /*xử lý đọc âm thanh của từ khi người dùng click vào biểu tượng loa*/
        var id = 1
        val helper = DatabaseHelper(context)
        helper.openDatabase()
        val db = helper.readableDatabase
        val query = "select id from dictionary where word = ? "
        val res = db.rawQuery(query, arrayOf(word), null)
        if (res.moveToFirst()) {
            id = res.getInt(0)
            Log.d("ID PLAY", "$id")
        }
        res.close()
        val mediaPlayer = MediaPlayer()
        val audioUrl = "http://157.10.45.121/audio/$id.mp3"
        try {
            // Đặt nguồn âm thanh từ URL
            mediaPlayer.setDataSource(audioUrl)

            // Chuẩn bị âm thanh không làm gián đoạn UI
            mediaPlayer.prepareAsync()

            // Khi âm thanh đã sẵn sàng, bắt đầu phát
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()  // Phát âm thanh
            }

            // Xử lý khi có lỗi xảy ra trong quá trình phát
            mediaPlayer.setOnErrorListener { _, _, _ ->
                Toast.makeText(context, "Lỗi khi phát âm thanh", Toast.LENGTH_SHORT).show()
                true  // Trả về true để chỉ ra lỗi đã được xử lý
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi tải âm thanh", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hobbyWord(query: String) {
        val helper = DatabaseHelper(context)
        val db = try {
            helper.openDatabase()
            helper.readableDatabase
        } catch (e: Exception) {
            Log.e("DATABASE", "Error opening database", e)
            Toast.makeText(context, "Lỗi khi mở cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val list = ArrayList<SaveDetailWord>()
            val res = db.rawQuery(
                "SELECT * FROM dictionary WHERE word = ?",
                arrayOf(query)
            )

            if (res.moveToFirst()) {
                do {
                    val word = res.getString(res.getColumnIndexOrThrow("word"))
                    val ipa = res.getString(res.getColumnIndexOrThrow("ipa"))
                    val type = res.getString(res.getColumnIndexOrThrow("type"))
                    val definition = res.getString(res.getColumnIndexOrThrow("definition"))
                    
                    list.add(
                        SaveDetailWord(
                            word ?: "Unknown",
                            "[${ipa ?: "Unknown IPA"}]",
                            R.drawable.loa,
                            R.drawable.star_hobby,
                            type ?: "Unknown Type",
                            definition ?: "No definition"
                        )
                    )

                    val contentValues = ContentValues().apply {
                        put("word", word)
                        put("ipa", ipa)
                        put("type", type)
                        put("definition", definition)
                    }

                    val checkCursor = db.rawQuery(
                        "SELECT * FROM hobby_word WHERE word = ?",
                        arrayOf(word)
                    )

                    try {
                        if (checkCursor.count == 0) {
                            val result = db.insert("hobby_word", null, contentValues)
                            if (result != -1L) {
                                Log.d("INSERT", "Đã thêm từ '$word' vào danh sách yêu thích")
                                Toast.makeText(context, "Đã thêm từ '$word' vào danh sách yêu thích", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("INSERT", "Không thể thêm từ '$word'")
                                Toast.makeText(context, "Không thể thêm từ '$word'", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.d("INSERT", "Từ '$word' đã tồn tại trong danh sách yêu thích")
                            Toast.makeText(context, "Từ '$word' đã có trong danh sách yêu thích", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        checkCursor.close()
                    }
                } while (res.moveToNext())
            }
            res.close()

            try {
                if (::adt.isInitialized) {
                    adt.updateData(list)
                    adt.notifyDataSetChanged()
                } else {
                    Log.e("ADAPTER", "MainDetailWord adapter chưa được khởi tạo")
                }
            } catch (e: Exception) {
                Log.e("ADAPTER", "Lỗi khi cập nhật adapter", e)
            }

        } catch (e: Exception) {
            Log.e("DATABASE", "Lỗi khi thao tác với cơ sở dữ liệu", e)
            Toast.makeText(context, "Đã xảy ra lỗi khi xử lý dữ liệu", Toast.LENGTH_SHORT).show()
        } finally {
            db.close()
        }
    }


    private fun deleteItem(word: String) {
        val helper = DatabaseHelper(context)
        helper.openDatabase()
        val db = helper.readableDatabase
        val res = db.delete("recent_word", "word = ?", arrayOf(word))
        if (res > 0) {
            // Cập nhật lại danh sách từ
            wordList = wordList.filter { it.word != word }
            // Thông báo cho adapter cập nhật lại giao diện
            notifyDataSetChanged()

            Toast.makeText(context, "Xoá thành công từ $word", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Xoá thất bại", Toast.LENGTH_SHORT).show()
        }
        db.close()
    }
}
