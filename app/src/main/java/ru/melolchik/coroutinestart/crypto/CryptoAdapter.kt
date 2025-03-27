package ru.melolchik.coroutinestart.crypto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.melolchik.coroutinestart.cript.Currency
import ru.melolchik.coroutinestart.databinding.CryptoItemBinding

class CryptoAdapter : ListAdapter<Currency,CryptoAdapter.CryptoViewHolder>(CryptoDiff){

    class CryptoViewHolder(val binding : CryptoItemBinding) : RecyclerView.ViewHolder(binding.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryptoViewHolder {
        val binding = CryptoItemBinding.inflate(LayoutInflater.from(parent.context),
            parent,false)
        return CryptoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CryptoViewHolder, position: Int) {
        val currency = getItem(position)
        holder.binding.textViewCurrencyName.text = currency.name
        holder.binding.textViewCurrencyPrice.text = currency.price.toString()
    }

    private object CryptoDiff : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.name == newItem.name;
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem == newItem;
        }

    }
}