package com.qian.wesmile.api.com.qian.wx.api.usermanagement;

import com.qian.wesmile.api.APITestBase;
import com.qian.wesmile.api.usermanagement.GetUserBasicInformation;
import com.qian.wesmile.model.param.UserTag;
import com.qian.wesmile.model.result.GetOpenId;
import org.junit.Test;

public class GetUserBasicInformationTest extends APITestBase<GetUserBasicInformation> {

    @Test
    public void oauth2() {
        UserTag userTag = new UserTag();
        UserTag.TagBean tagBean = new UserTag.TagBean();
        tagBean.setId(100);
        tagBean.setName(Math.random() + "");
        userTag.setTag(tagBean);
        GetOpenId authorization_code = api.oauth2("","", "011jXYAb1knvWw0jcIAb1RdMAb1jXYAK", "authorization_code");

    }

    @Test
    public void snsUserInfo() {
        //这里的accessToken应该是oauth2这个接口中返回的accessToken
        api.snsUserInfo("aa", "oVq-Jwh__sCYrMO3EekXFtx09wdQ", "zh_CN");

    }
}
