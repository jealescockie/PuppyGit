package com.catpuppyapp.puppygit.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.compose.LoadingDialog
import com.catpuppyapp.puppygit.compose.LongPressAbleIconBtn
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.data.entity.CredentialEntity
import com.catpuppyapp.puppygit.data.entity.RepoEntity
import com.catpuppyapp.puppygit.dev.dev_EnableUnTestedFeature
import com.catpuppyapp.puppygit.dev.shallowAndSingleBranchTestPassed
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.user.UserUtil
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.Libgit2Helper.Companion.getGitUrlType
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.addPrefix
import com.catpuppyapp.puppygit.utils.boolToDbInt
import com.catpuppyapp.puppygit.utils.checkFileOrFolderNameAndTryCreateFile
import com.catpuppyapp.puppygit.utils.dbIntToBool
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.getRepoNameFromGitUrl
import com.catpuppyapp.puppygit.utils.isPathExists
import com.catpuppyapp.puppygit.utils.state.StateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val stateKeyTag = "CloneScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    repoId: String?,  //编辑已存在仓库的时候，用得着这个
    naviUp: () -> Boolean,
) {

    val TAG = "CloneScreen"

    val appContext = LocalContext.current
    val inDarkTheme = Theme.inDarkTheme

    SideEffect {
        Msg.msgNotifyHost()
    }

    val isEditMode = StateUtil.getRememberSaveableState(initValue = false)
    val repoFromDb = StateUtil.getCustomSaveableState(keyTag=stateKeyTag, keyName = "repoFromDb", initValue = RepoEntity(id = ""))
    //克隆完成后更新此变量，然后在重新渲染时直接返回。（注：因为无法在coroutine里调用naviUp()，所以才这样实现“存储完成返回上级页面”的功能）
//    val isTimeNaviUp = rememberSaveable { mutableStateOf(false) }
//
//    if(isTimeNaviUp.value) {
//        naviUp()
//    }

//    val userIsPro = UserInfo.isPro()

    val homeTopBarScrollBehavior = AppModel.singleInstanceHolder.homeTopBarScrollBehavior

    val allRepoParentDir = AppModel.singleInstanceHolder.allRepoParentDir

    val gitUrl = StateUtil.getRememberSaveableState(initValue = "")
//    val repoName = remember { mutableStateOf(TextFieldValue("")) }
    val repoName = StateUtil.getCustomSaveableState(keyTag=stateKeyTag, keyName = "repoName",  initValue = TextFieldValue(""))
    val branch = StateUtil.getRememberSaveableState(initValue = "")
    val depth = StateUtil.getRememberSaveableState(initValue = "")  //默认depth 为空，克隆全部；不为空则尝试解析，大于0，则传给git；小于0则克隆全部
//    val credentialName = remember { mutableStateOf(TextFieldValue("")) }  //旋转手机，画面切换后值会被清，因为不是 rememberSaveable，不过rememberSaveable不适用于TextFieldValue，所以改用我写的自定义状态存储器了
    val credentialName = StateUtil.getCustomSaveableState(keyTag=stateKeyTag, keyName = "credentialName", initValue = TextFieldValue(""))
    val credentialVal = StateUtil.getRememberSaveableState(initValue = "")
    val credentialPass = StateUtil.getRememberSaveableState(initValue = "")

    val gitUrlType = StateUtil.getRememberSaveableIntState(initValue = Cons.gitUrlTypeHttp)

    val curCredentialType = StateUtil.getRememberSaveableIntState(initValue = Cons.dbCredentialTypeHttp)
//    val credentialListHttp = MockData.getAllCredentialList(type = Cons.dbCredentialTypeHttp)
//    val credentialListSsh = MockData.getAllCredentialList(type = Cons.dbCredentialTypeSsh)
    val credentialHttpList = StateUtil.getCustomSaveableStateList(keyTag=stateKeyTag, keyName = "credentialHttpList", initValue = listOf<CredentialEntity>())
    val credentialSshList = StateUtil.getCustomSaveableStateList(keyTag=stateKeyTag, keyName = "credentialSshList", initValue = listOf<CredentialEntity>())
    //这个用我写的自定义状态存储器没意义，因为如果屏幕旋转（手机的显示设置改变），本质上就会重新创建组件，重新加载列表，除非改成如果列表不为空，就不查询，但那样意义不大
//    val curCredentialList:SnapshotStateList<CredentialEntity> = remember { mutableStateListOf() }  //切换http和ssh后里面存对应的列表

    val selectedCredentialId= StateUtil.getRememberSaveableState(initValue = "")
    val selectedCredentialName= StateUtil.getRememberSaveableState(initValue = "")

    //获取输入焦点，弹出键盘
    val focusRequesterGitUrl = StateUtil.getRememberStateRawValue(initValue = FocusRequester())  // 1
    val focusRequesterRepoName = StateUtil.getRememberStateRawValue(initValue = FocusRequester())  // 2
    val focusRequesterCredentialName = StateUtil.getRememberStateRawValue(initValue = FocusRequester())  // 3
    val focusToNone = 0
    val focusToGitUrl = 1;
    val focusToRepoName = 2;
    val focusToCredentialName = 3;
    val requireFocusTo = StateUtil.getRememberSaveableIntState(initValue = focusToNone)  //初始值0谁都不聚焦，修改后的值： 1聚焦url；2聚焦仓库名；3聚焦凭据名

    val noCredential = stringResource(R.string.no_credential)
    val newCredential = stringResource(R.string.new_credential)
    val selectCredential = stringResource(R.string.select_credential)

    val optNumNoCredential = 0  //这个值就是对应的选项在选项列表的索引
    val optNumNewCredential = 1
    val optNumSelectCredential = 2
    val credentialRadioOptions = listOf(noCredential, newCredential, selectCredential)  // 编号: 文本
    val (credentialSelectedOption, onCredentialOptionSelected) = StateUtil.getRememberSaveableIntState(initValue = optNumNoCredential)

    val (isRecursiveClone, onIsRecursiveCloneStateChange) = StateUtil.getRememberSaveableState(false)
    val (isSingleBranch, onIsSingleBranchStateChange) = StateUtil.getRememberSaveableState(false)

    val isReadyForClone = StateUtil.getRememberSaveableState(false)

    val passwordVisible =StateUtil.getRememberSaveableState(false)

    val dropDownMenuExpendState = StateUtil.getRememberSaveableState(false)

    val showRepoNameAlreadyExistsErr = StateUtil.getRememberSaveableState(false)
    val showCredentialNameAlreadyExistsErr =StateUtil.getRememberSaveableState(false)
    val showRepoNameHasIllegalCharsOrTooLongErr = StateUtil.getRememberSaveableState(false)

    val updateRepoName:(TextFieldValue)->Unit = {
        val newVal = it
        val oldVal = repoName.value

        //只有当值改变时，才解除输入框报错
        if(oldVal.text != newVal.text) {
            //用户一改名，就取消字段错误设置，允许点击克隆按钮，点击后再次检测，有错再设置为真
            showRepoNameAlreadyExistsErr.value = false
            showRepoNameHasIllegalCharsOrTooLongErr.value = false
        }

        //这个变量必须每次都更新，不能只凭text是否相等来判断是否更新此变量，因为选择了哪些字符、光标在什么位置 等信息也包含在这个TextFieldValue对象里
        repoName.value = newVal

    }
    val updateCredentialName:(TextFieldValue)->Unit = {
        val newVal = it
        val oldVal = credentialName.value

        if(oldVal.text != newVal.text) {
            if (showCredentialNameAlreadyExistsErr.value) {
                showCredentialNameAlreadyExistsErr.value = false
            }
        }

        credentialName.value = newVal
    }
    val focusRepoName:()->Unit = {
        //全选输入框字符
        val text = repoName.value.text
        repoName.value = repoName.value.copy(
            selection = TextRange(0, text.length)
        )

        //聚焦输入框，弹出键盘（如果本身光标就在输入框，则不会弹出键盘）
//        focusRequesterRepoName.requestFocus()
        requireFocusTo.intValue = focusToRepoName
    }
    val setCredentialNameExistAndFocus:()->Unit = {
        //设置错误state
        showCredentialNameAlreadyExistsErr.value=true

        //全选输入框字符
        val text = credentialName.value.text
        credentialName.value = credentialName.value.copy(
            //设置选择范围
            selection = TextRange(0, text.length)
        )

        //另一种写法，测试过，可行，上面的copy方法内部其实就是这么实现的，差不多
//        credentialName.value = TextFieldValue(text = credentialName.value.text,
//                                          selection = TextRange(0, credentialName.value.text.length)
//                                          )

        //聚焦输入框，弹出键盘（如果本身光标就在输入框，则不会弹出键盘）
//        focusRequesterCredentialName.requestFocus()
        requireFocusTo.intValue = focusToCredentialName
    }

    val showLoadingDialog = StateUtil.getRememberSaveableState(false)

    val doSave:()->Unit = {
        /*查询repo名以及repo在仓库存储目录是否已经存在，若存在，设 isReadyForClone为假，调用setRepoNameExistAndFocus()提示用户改名
            查询credentialName是否已经存在，若存在，设 isReadyForClone为假，调用setCredentialNameExistAndFocus()提示用户改名
            通过检测以后，若是newCredential则存储credential
            存储仓库信息，设置仓库状态为notReadyNeedClone，然后返回仓库页面

            下一步，但不在此页面执行：仓库页面检查仓库状态，对所有状态为notReadyNeedClone的仓库执行clone（可能会发生一个克隆未完成就执行另一个克隆的问题，需要考虑下怎么解决）
        */


        doJobThenOffLoading launch@{
            showLoadingDialog.value=true

            val repoNameText = repoName.value.text
            //检查是否存在非法字符，例如路径分隔符\:之类的
            val repoNameCheckRet = checkFileOrFolderNameAndTryCreateFile(repoNameText, appContext)
            if(repoNameCheckRet.hasError()) {
                Msg.requireShowLongDuration(repoNameCheckRet.msg)

                focusRepoName()
                showRepoNameHasIllegalCharsOrTooLongErr.value=true
                showLoadingDialog.value=false
                return@launch
            }

            val repoDb = AppModel.singleInstanceHolder.dbContainer.repoRepository
            val credentialDb = AppModel.singleInstanceHolder.dbContainer.credentialRepository
            //如果不是编辑模式 或者 是编辑模式但用户输入的仓库名不是当前仓库已经保存的名字 则 检查仓库名和文件夹是否已经存在
            if(!isEditMode.value || repoNameText != repoFromDb.value.repoName) {
                //检查仓库名是否已经存在
                val isRepoNameExist = repoDb.isRepoNameExist(repoNameText)
                //仓库在数据库存在或者路径已经存在，则报错
                if(isRepoNameExist || isPathExists(allRepoParentDir.canonicalPath, repoNameText)) {
                    focusRepoName()
                    showRepoNameAlreadyExistsErr.value=true
                    showLoadingDialog.value=false
                    return@launch
                }

            }


            var credentialIdForClone = ""  //如果选的是 no credential，则就是这个值，否则，会在下面的判断里更新其值为新增或选择的credentialid

            //如果选择的是新建Credential，则新建
            var credentialForSave:CredentialEntity? = null
            if(credentialSelectedOption==optNumNewCredential) {
                val credentialNameText = credentialName.value.text
                val isCredentialNameExist = credentialDb.isCredentialNameExist(credentialNameText)
                if(isCredentialNameExist) {
                    setCredentialNameExistAndFocus()
                    showLoadingDialog.value=false
                    return@launch
                }

                credentialForSave = CredentialEntity(name = credentialNameText,
                                                    value = credentialVal.value,
                                                    pass = credentialPass.value,
                                                    type = curCredentialType.intValue,

                )
                credentialDb.insertWithEncrypt(credentialForSave)

                //为仓库更新credentialId
                credentialIdForClone = credentialForSave.id
            } else if(credentialSelectedOption == optNumSelectCredential) {
                credentialIdForClone = selectedCredentialId.value
            }



            var intDepth = 0;
            var isShallow= Cons.dbCommonFalse
            if(depth.value.isNotBlank()) {
                try {  //如果在这不出错，intDepth大于等于0
                    //虽然输入限制了仅限数字，但用户依然可以粘贴非数字内容，所以parse还是有可能出错，因此需要try catch
                    //注：toInt内部调用的其实还是 Integer.parseInt()
                    //注：coerceAtLeast(0)确保解析出的数字不小于0
                    intDepth = depth.value.toInt().coerceAtLeast(0)
                }catch (e:Exception) {  //如果try代码块出错，intDepth将等于0
                    intDepth=0
                    Log.e(TAG,"invalid depth value, will use default value(0)")
                }

                //执行到这intDepth必然大于等于0，所以不需再判断
//                intDepth = if(intDepth>0) intDepth else 0  //避免intDepth小于0

                //执行到这intDepth必然大于等于0，等于0等于非shallow，大于0等于shallow(暂且等于，实际上如果其值大于所有提交数，最终仓库依然是非shallow状态)
                if(intDepth>0) {  //注：这里的状态只是预判，如果depth大于仓库实际的提交数，克隆后仓库依然是非shallow的，isShallow也会被更新为假，可通过检测仓库.git目录是否存在shallow文件来判断仓库是否处于shallowed状态，我已经在克隆仓库实现了这个功能
                    isShallow = Cons.dbCommonTrue
                }
            }

            //这里不用判断repoFromDb.id，如果没成功更新repoFromDb为数据库中的值，那它的id会是空字符串，不会匹配到任何记录，而isEditMode为true时，会执行update操作，是按id匹配的，所以，最终不会影响任何数据，顶多就是用户输入的内容没保存上而已。
            val repoForSave:RepoEntity = if(isEditMode.value) repoFromDb.value else RepoEntity(createBy = Cons.dbRepoCreateByClone);
            //设置repo字段
            repoForSave.repoName = repoNameText
            repoForSave.fullSavePath = allRepoParentDir.canonicalPath+ File.separator +repoNameText
            repoForSave.cloneUrl = gitUrl.value
            repoForSave.workStatus = Cons.dbRepoWorkStatusNotReadyNeedClone
            repoForSave.credentialIdForClone = credentialIdForClone
            repoForSave.isRecursiveCloneOn = boolToDbInt(isRecursiveClone)
            repoForSave.depth = intDepth
            repoForSave.isShallow = isShallow

            //设置分支和singlebranch字段
            //设置分支
            if(branch.value.isNotBlank()) {  //只有分支字段不为空时，才存储isSingleBranch的值，否则强制把isSingleBranch设置为关闭
                repoForSave.branch=branch.value
                repoForSave.isSingleBranch=boolToDbInt(isSingleBranch)
            }else{  //没填branch
                repoForSave.branch = ""
                repoForSave.isSingleBranch=Cons.dbCommonFalse  //没填branch，忽略isSingleBranch状态的值，强制设置为false
            }

            //编辑模式，更新，否则插入
            if(isEditMode.value){
                repoDb.update(repoForSave)
            }else{
                repoDb.insert(repoForSave)
            }
            showLoadingDialog.value=false

            //设置此变量，下次重新渲染就会直接返回上级页面了
//            isTimeNaviUp.value = true
            withContext(Dispatchers.Main) {
                naviUp()
            }
        }
    }
    val loadingText = StateUtil.getRememberSaveableState(initValue = appContext.getString(R.string.loading))

    val spacerPadding = 2.dp
    Scaffold(
        modifier = Modifier.nestedScroll(homeTopBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                        Text(stringResource(R.string.clone))
                },
                navigationIcon = {
                    LongPressAbleIconBtn(
                        tooltipText = stringResource(R.string.back),
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        iconContentDesc = stringResource(R.string.back),

                        ) {
                        naviUp()
                    }
                },
                actions = {
                    LongPressAbleIconBtn(
                        tooltipText = stringResource(R.string.save),
                        icon =  Icons.Filled.Check,
                        iconContentDesc = stringResource(id = R.string.save),
                        enabled = isReadyForClone.value,

                        ) {
                        doSave()
                    }
                },
                scrollBehavior = homeTopBarScrollBehavior,
            )
        },
    ){contentPadding->
        //遮罩loading，这里不用做if loading else show page 的判断，直接把loading遮盖在页面上即可
//        showLoadingDialog.value=true  //test
        if (showLoadingDialog.value) {
            LoadingDialog(loadingText.value)
        }

        Column (modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .verticalScroll(StateUtil.getRememberScrollState())
            .padding(bottom = MyStyleKt.Padding.PageBottom)  //这个padding是为了使密码框不在底部，类似vscode中文件的最后一行也可滑到屏幕中间一样的意义
        ){
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .focusRequester(focusRequesterGitUrl),
                singleLine = true,

                value = gitUrl.value,
                onValueChange = {
                    gitUrl.value=it
                    val repoNameFromGitUrl:String = getRepoNameFromGitUrl(it)
                    if(repoNameFromGitUrl.isNotBlank()) {
                        updateRepoName(TextFieldValue(repoNameFromGitUrl))
                    }

                    //获取当前凭据类型并检查是否发生了变化，如果变化，需要清些字段
                    val newGitUrlType = getGitUrlType(it)  //获取当前url类型（http or ssh）

                    // 20240414 废弃ssh支持，修改开始
//                    val newCredentialType = getCredentialTypeByGitUrlType(newGitUrlType)  //根据url类型获取credential类型（http or ssh）  //ssh
                    val newCredentialType = Cons.dbCredentialTypeHttp  //nossh
                    // 20240414 废弃ssh支持，修改结束

                    val oldCredentialType = curCredentialType.intValue

                    //更新凭据相关字段
                    //credentialName.value=""  //title没必要清，ssh和http的title通用，只是用于标识凭据的名称而已
//                    credentialVal.value=""  //没必要清，用户觉得不对，他自己全选删除不就行了？
//                    credentialPass.value=""  //这个也没必要清，理由同上
                    //类型改变时需要重置一些字段
                    if(newCredentialType!=oldCredentialType) {  //为true代表url类型改变了，credential类型也需要跟着改变，并且重置一些状态变量
                        //选择凭据相关字段，这两个有必要清，因为类型一换，凭据列表就变了，而且不同类型的凭据也不通用，所以这个得在凭据类型改变时清一下
                        selectedCredentialName.value=""
                        selectedCredentialId.value=""
                        //如果url类型改变 且 凭据选的是选择凭据，则将其改为无凭据，因为ssh和http的凭据不通用
                        if(credentialSelectedOption == optNumSelectCredential) {  //如果当前是选择凭据，则改成无凭据（若是无凭据或新建凭据，则不执行操作）
                            onCredentialOptionSelected(optNumNoCredential)
                        }
                    }

                    //更新状态，最好在最后更新状态，感觉在上面更新，如果渲染周期。。。不，应该也不会有问题，总之就在这更新吧
                    //更新凭据类型和giturl状态变量
                    curCredentialType.intValue = newCredentialType
                    gitUrlType.intValue = newGitUrlType

                },
                label = {
                    Row {
                        Text(stringResource(R.string.git_url))
                        Text(text = " ("+stringResource(id = R.string.only_https_supported)+")",
//                            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, end = 10.dp),
//                            fontSize = 11.sp

                        )

                    }
                },
                placeholder = {
                    Text(stringResource(R.string.git_url_placeholder))
                }
            )
            Spacer(modifier = Modifier.padding(spacerPadding))
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .focusRequester(focusRequesterRepoName)
                ,
                value = repoName.value,
                singleLine = true,
                isError = showRepoNameAlreadyExistsErr.value || showRepoNameHasIllegalCharsOrTooLongErr.value,
                supportingText = {
                    val errMsg = if(showRepoNameAlreadyExistsErr.value) stringResource(R.string.repo_name_exists_err)
                                else if(showRepoNameHasIllegalCharsOrTooLongErr.value) stringResource(R.string.err_repo_name_has_illegal_chars_or_too_long)
                                else ""

                    if (showRepoNameAlreadyExistsErr.value || showRepoNameHasIllegalCharsOrTooLongErr.value) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = errMsg,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    val errMsg = if(showRepoNameAlreadyExistsErr.value) stringResource(R.string.repo_name_exists_err)
                                else if(showRepoNameHasIllegalCharsOrTooLongErr.value) stringResource(R.string.err_repo_name_has_illegal_chars_or_too_long)
                                else ""
                    if (showRepoNameAlreadyExistsErr.value || showRepoNameHasIllegalCharsOrTooLongErr.value) {
                        Icon(imageVector=Icons.Filled.Error,
                            contentDescription=errMsg,
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                //TODO 可选：如果可以的话，检查下用户是否手动在这里输入过文件夹名，若输过，即使url改变，也不再自动改变值。另外，用户手动输入的值依然会被检测是否存在路径以及路径名是否有坏字符
                onValueChange = {
                    updateRepoName(it)
                },
                label = {
                    Text(stringResource(R.string.local_repo_name))
                },
                placeholder = {
                    Text(stringResource(R.string.local_repo_name_placeholder))
                }
            )
            Spacer(modifier = Modifier.padding(spacerPadding))

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

                value = branch.value,
                singleLine = true,

                onValueChange = {
                    branch.value=it
                },
                label = {
                    Text(stringResource(R.string.branch_optional))
                },
                placeholder = {
                    Text(stringResource(R.string.branch_name))
                }
            )

            //开发者模式 或 功能测试通过且用户是pro付费用户，则启用depth和singlebranch功能
//            改成启用未测试特性和 is shallowpassed控制功能是否显示，isPro决定功能是否enabled以及显示不同的文案( eg: depth and depth(Pro))
            if(dev_EnableUnTestedFeature || shallowAndSingleBranchTestPassed) {
                val isPro = UserUtil.isPro()
                val enableSingleBranch =  branch.value.isNotBlank() && isPro
                //single branch checkbox 开始
                //single branch选择框，如果branch值不为空，则可以启用或禁用，如果branch值为空，checkbox状态本身不变，但存储时忽略其值，默认当成禁用。
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(MyStyleKt.CheckoutBox.height)
                        .toggleable(
                            enabled = enableSingleBranch,
                            value = isSingleBranch,
                            onValueChange = { onIsSingleBranchStateChange(!isSingleBranch) },
                            role = Role.Checkbox
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        enabled = enableSingleBranch,
                        checked = isSingleBranch,
                        onCheckedChange = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = if(isPro) stringResource(R.string.single_branch) else stringResource(R.string.single_branch_pro_only),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                        color = if(enableSingleBranch) Color.Unspecified else if(inDarkTheme) MyStyleKt.TextColor.disable_DarkTheme else MyStyleKt.TextColor.disable
                    )
                }
                //single branch checkbox 结束

                Spacer(modifier = Modifier.padding(spacerPadding))
//      depth输入框开始
                //20240414 测试 发现depth有问题，虽能成功克隆，但之后莫名其妙出问题提示找不到object id的概率非常大！而且在手机处理不了(不过电脑上的git虽也报错但能正常pull/push)，考虑过后，决定暂时放弃支持depth功能
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = isPro,
                    value = depth.value,
                    onValueChange = {
                        depth.value=it
                    },
                    label = {
                        if(isPro) Text(stringResource(R.string.depth_optional)) else Text(stringResource(R.string.depth_optional_pro_only))
                    },
                    placeholder = {
                        Text(stringResource(R.string.depth))
                    }
                )
                Spacer(modifier = Modifier.padding(spacerPadding))

                //depth输入框结束

            }

            //递归克隆checkbox开始
            //暂不支持递归克隆
            /*
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(MyStyleKt.CheckoutBox.height)
                    .toggleable(
                        value = isRecursiveClone,
                        onValueChange = { onIsRecursiveCloneStateChange(!isRecursiveClone) },
                        role = Role.Checkbox
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isRecursiveClone,
                    onCheckedChange = null // null recommended for accessibility with screenreaders
                )
                Text(
                    text = stringResource(R.string.recursive_clone),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        */
            //递归克隆checkbox结束


            HorizontalDivider(modifier = Modifier.padding(spacerPadding))
            //choose credential
            Column(modifier = Modifier.selectableGroup(),

                ) {
                var end = credentialRadioOptions.size-1
                //如果对应类型的集合为空，就不显示“选择凭据”选项了
                if((curCredentialType.intValue == Cons.dbCredentialTypeHttp && credentialHttpList.value.isEmpty())
                    ||(curCredentialType.intValue==Cons.dbCredentialTypeSsh && credentialSshList.value.isEmpty())) {
                    end-=1
                }

                //如果设置了有效gitUrl，显示新建和选择凭据，否则只显示无凭据
                for(k in 0..end){
                    val optext = credentialRadioOptions[k]

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = MyStyleKt.RadioOptions.minHeight)

                            .selectable(
                                selected = (credentialSelectedOption == k),
                                onClick = {
                                    //更新选择值
                                    onCredentialOptionSelected(k)
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (credentialSelectedOption == k),
                            onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            text = optext,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
            if(credentialSelectedOption == optNumNewCredential) {
                //显示新建credential的输入框
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .focusRequester(focusRequesterCredentialName)
                    ,
                    isError = showCredentialNameAlreadyExistsErr.value,
                    supportingText = {
                        if (showCredentialNameAlreadyExistsErr.value) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.credential_name_exists_err),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (showCredentialNameAlreadyExistsErr.value)
                            Icon(imageVector=Icons.Filled.Error,
                                contentDescription= stringResource(R.string.credential_name_exists_err),
                                tint = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,

                    value = credentialName.value,
                    onValueChange = {
                        updateCredentialName(it)
                    },
                    label = {
                        Text(stringResource(R.string.credential_name))
                    },
                    placeholder = {
                        Text(stringResource(R.string.credential_name_placeholder))
                    }
                )
                TextField(
                    modifier =
                    //如果type是ssh，让private-key输入框高点
                    if(curCredentialType.intValue == Cons.dbCredentialTypeSsh) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 300.dp)
                            .padding(10.dp)

                    }else{
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    }
                        ,
                    singleLine = curCredentialType.intValue != Cons.dbCredentialTypeSsh,

                    value = credentialVal.value,
                    onValueChange = {
                        credentialVal.value=it
                    },
                    label = {
                        if(curCredentialType.intValue == Cons.dbCredentialTypeSsh) {
                            Text(stringResource(R.string.private_key))
                        }else{
                            Text(stringResource(R.string.username))
                        }
                    },
                    placeholder = {
                        if(curCredentialType.intValue == Cons.dbCredentialTypeSsh) {
                            Text(stringResource(R.string.paste_your_private_key_here))
                        }else{
                            Text(stringResource(R.string.username))
                        }
                    }
                )
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    singleLine = true,
                    value = credentialPass.value,
                    onValueChange = {
                        credentialPass.value=it
                    },
                    label = {
                        if(curCredentialType.intValue == Cons.dbCredentialTypeSsh) {
                            Text(stringResource(R.string.passphrase_if_have))
                        }else{
                            Text(stringResource(R.string.password))
                        }
                    },
                    placeholder = {
                        if(curCredentialType.intValue == Cons.dbCredentialTypeSsh) {
                            Text(stringResource(R.string.input_passphrase_if_have))
                        }else{
                            Text(stringResource(R.string.password))
                        }
                    },
                    visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible.value) Icons.Filled.Visibility
                                    else Icons.Filled.VisibilityOff

                        // Please provide localized description for accessibility services
                        val description = if (passwordVisible.value) stringResource(R.string.hide_password) else stringResource(R.string.show_password)

                        IconButton(onClick = {passwordVisible.value = !passwordVisible.value}){
                            // contentDescription is for accessibility
                            Icon(imageVector=image, contentDescription=description)
                        }
                    }
                )
            }else if(credentialSelectedOption == optNumSelectCredential) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center),

                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){

                    Row {  //让按钮和下拉菜单近点
                        Button(onClick = { dropDownMenuExpendState.value = true }) {
                            Text(stringResource(R.string.tap_for_select_credential))
                        }
                        //查询所有凭据，显示下拉选择框(selector)
                        DropdownMenu(
                            expanded = dropDownMenuExpendState.value,
                            onDismissRequest = { dropDownMenuExpendState.value = false }
                        ) {
                            val curList = if(curCredentialType.intValue == Cons.dbCredentialTypeHttp) credentialHttpList else credentialSshList
                            for(item in curList.value.toList()) {
                                val itemText = if(item.id == selectedCredentialId.value) addPrefix(item.name) else item.name
                                DropdownMenuItem(
                                    text = { Text(itemText) },
                                    onClick = {
                                        selectedCredentialId.value = item.id
                                        selectedCredentialName.value = item.name
                                        dropDownMenuExpendState.value=false
                                    }
                                )

                            }
                        }
                    }
                    Row {
                        Text(stringResource(R.string.selected_credential))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text =selectedCredentialName.value,
                            fontWeight = FontWeight.ExtraBold

                        )
                    }
                }
            }
        }
    }

    if(requireFocusTo.intValue==focusToGitUrl) {
        requireFocusTo.intValue=focusToNone
        focusRequesterGitUrl.requestFocus()
    }else if(requireFocusTo.intValue==focusToRepoName) {
        requireFocusTo.intValue=focusToNone
        focusRequesterRepoName.requestFocus()
    }else if(requireFocusTo.intValue==focusToCredentialName) {
        requireFocusTo.intValue=focusToNone
        focusRequesterCredentialName.requestFocus()
    }

    LaunchedEffect(Unit) {
//        MyLog.d(TAG, "#LaunchedEffect: repoId=" + repoId)
        //编辑已存在repo
        // TODO 设置页面loading为true
        //      从数据库异步查询repo数据，更新页面state
        //      设置页面loading 为false
        doJobThenOffLoading(
            loadingOn = { showLoadingDialog.value = true },
            loadingOff = { showLoadingDialog.value = false }
        ) job@{
            if (repoId != null && repoId.isNotBlank() && repoId != "null") {  //如果是编辑模式，查询仓库信息
                isEditMode.value = true
                val repoDb = AppModel.singleInstanceHolder.dbContainer.repoRepository
                val credentialDb = AppModel.singleInstanceHolder.dbContainer.credentialRepository
                val repo = repoDb.getById(repoId) ?: return@job
                gitUrlType.intValue = getGitUrlType(repo.cloneUrl)  //更新下giturl type
                gitUrl.value = repo.cloneUrl
                repoName.value = TextFieldValue(repo.repoName)
                branch.value = repo.branch
                //设置是否单分支
                onIsSingleBranchStateChange(dbIntToBool(repo.isSingleBranch))
                //设置是否递归克隆
                onIsRecursiveCloneStateChange(dbIntToBool(repo.isRecursiveCloneOn))
                //depth只有大于0时设置才有意义
                if (repo.depth > 0) {
                    depth.value = "" + repo.depth
                }

                //把repo存到状态变量，保存时就不用再查询了
                repoFromDb.value = repo

                //检查是否存在credential，如果存在，设置下相关状态变量
                val credentialIdForClone = repo.credentialIdForClone
                //注意，如果仓库存在关联的credential，在克隆页面编辑仓库时，不能编辑credential，只能新建或选择之前的credential，若想编辑credential，需要去credential页面，这样是为了简化实现逻辑
                if (!credentialIdForClone.isNullOrBlank()) {  //更新credential相关字段
                    val credential = credentialDb.getById(credentialIdForClone)
                    MyLog.d(TAG, "#LaunchedEffect:credential==null:" +(credential==null))

                    if (credential == null) {  //要么没设置，要么设置了但被删除了，所以是无效id，这两种情况都会查不出对应的credential
                        onCredentialOptionSelected(optNumNoCredential)
                    } else {  //存在之前设置的credential
                        //设置选中的credential
                        onCredentialOptionSelected(optNumSelectCredential)  //选中“select credential”单选项
                        selectedCredentialName.value = credential.name  //选中项的名字，显示给用户看的
                        selectedCredentialId.value = credential.id  //选中项的id，保存时用的，不给用户看

                        curCredentialType.intValue = credential.type  //设置当前credential类型
                    }
                }
            } else {  //如果是新增模式，简单聚焦下第一个输入框，弹出键盘即可
                isEditMode.value = false
                //聚焦第一个输入框，算了不聚焦了，在协程里聚焦会报异常，虽然可以设置状态，然后在compose里判断，聚焦，再把状态关闭，但是，太麻烦了，而且感觉聚焦与否其实意义不大，甚至就连报错时的聚焦意义都不大，不过报错时的聚焦不需要在协程里执行也不会抛异常，所以暂且保留
                requireFocusTo.intValue = focusToGitUrl
            }

            //查询credential列表，无论新增还是编辑都需要查credential列表
            val credentialDb = AppModel.singleInstanceHolder.dbContainer.credentialRepository
            credentialHttpList.value.clear()
            credentialSshList.value.clear()

            //注：这里不需要显示密码，只是列出已保存的凭据供用户选择，顶多需要个凭据名和凭据id，所以查询的是未解密密码的list
            credentialHttpList.value.addAll(credentialDb.getHttpList())
            credentialSshList.value.addAll(credentialDb.getSshList())

//            credentialHttpList.requireRefreshView()
//            credentialSshList.requireRefreshView()
            MyLog.d(TAG, "#LaunchedEffect:credentialHttpList.size=" + credentialHttpList.value.size + ", credentialSshList.size=" + credentialSshList.value.size)

        }
    }



    //判定是否启用执行克隆的按钮，每次状态改变重新渲染页面都会执行这段代码更新此值
    isReadyForClone.value = ((gitUrl.value.isNotBlank() && repoName.value.text.isNotBlank())
        &&
        ((credentialSelectedOption==optNumNoCredential)  //新凭据的情况
                || ((credentialSelectedOption==optNumNewCredential && credentialName.value.text.isNotBlank())  //必填字段
                    //要么是http且填了密码字段，要么是ssh且填了privatekey字段
                    && (curCredentialType.intValue==Cons.dbCredentialTypeHttp && credentialPass.value.isNotBlank()) || (curCredentialType.intValue==Cons.dbCredentialTypeSsh && credentialVal.value.isNotBlank())
                   )
                || (credentialSelectedOption==optNumSelectCredential && selectedCredentialId.value.isNotBlank() && selectedCredentialName.value.isNotBlank()))
        && !showRepoNameAlreadyExistsErr.value && !showRepoNameHasIllegalCharsOrTooLongErr.value && !showCredentialNameAlreadyExistsErr.value
        )

}

