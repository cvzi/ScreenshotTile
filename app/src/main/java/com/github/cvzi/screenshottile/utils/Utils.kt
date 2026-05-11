package com.github.cvzi.screenshottile.utils

/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

const val UTILSKT = "Utils.kt"

/**
 * Returns true if this set contains the specified CharsSequence as a String.
 */
fun HashSet<String>.contains(seq: CharSequence): Boolean = this.contains(seq.toString())
