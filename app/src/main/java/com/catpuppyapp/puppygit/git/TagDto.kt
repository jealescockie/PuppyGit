package com.catpuppyapp.puppygit.git

import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import java.time.OffsetDateTime

class TagDto (
    var name:String="",
    var shortName:String="",
    var fullOidStr:String="",   // see below annotation of `targetFullOidStr`
    var targetFullOidStr:String="",  // if "isAnnotated" is false, this equals fullOidStr, else this is commit's oid, fullOidStr is Tag's oid
    var isAnnotated:Boolean=false,

    // below only make sense for annotated tags
    var taggerName:String="",
    var taggerEmail:String="",
    var date:OffsetDateTime?=null,
    var msg:String=""
) {

    private var targetShortOidStr:String?=null

    fun getFormattedTaggerNameAndEmail():String {
        return Libgit2Helper.getFormattedUsernameAndEmail(taggerName, taggerEmail)
    }

    fun getFormattedDate():String {
        return date?.format(Cons.defaultDateTimeFormatter) ?: ""
    }

    fun getType():String {
        val activityContext = AppModel.activityContext

        return if(isAnnotated) activityContext.getString(R.string.annotated) else activityContext.getString(R.string.lightweight)
    }

    fun getCachedTargetShortOidStr():String {
        if(targetShortOidStr==null) {
            targetShortOidStr=Libgit2Helper.getShortOidStrByFull(targetFullOidStr)
        }

        return targetShortOidStr ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagDto

        if (name != other.name) return false
        if (shortName != other.shortName) return false
        if (fullOidStr != other.fullOidStr) return false
        if (targetFullOidStr != other.targetFullOidStr) return false
        if (isAnnotated != other.isAnnotated) return false
        if (taggerName != other.taggerName) return false
        if (taggerEmail != other.taggerEmail) return false
        if (date != other.date) return false
        if (msg != other.msg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + shortName.hashCode()
        result = 31 * result + fullOidStr.hashCode()
        result = 31 * result + targetFullOidStr.hashCode()
        result = 31 * result + isAnnotated.hashCode()
        result = 31 * result + taggerName.hashCode()
        result = 31 * result + taggerEmail.hashCode()
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + msg.hashCode()
        return result
    }

    override fun toString(): String {
        return "TagDto(name='$name', shortName='$shortName', fullOidStr='$fullOidStr', targetFullOidStr='$targetFullOidStr', isAnnotated=$isAnnotated, taggerName='$taggerName', taggerEmail='$taggerEmail', date=$date, msg='$msg')"
    }


}
