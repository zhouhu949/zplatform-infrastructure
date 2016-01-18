/* 
 * MemberAccountServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年1月15日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.member.service.impl;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlebank.zplatform.acc.bean.AccEntry;
import com.zlebank.zplatform.acc.bean.Account;
import com.zlebank.zplatform.acc.bean.enums.CRDRType;
import com.zlebank.zplatform.acc.bean.enums.Usage;
import com.zlebank.zplatform.acc.exception.AbstractBusiAcctException;
import com.zlebank.zplatform.acc.service.AccountService;
import com.zlebank.zplatform.acc.service.BusiAcctService;
import com.zlebank.zplatform.commons.bean.DefaultPageResult;
import com.zlebank.zplatform.commons.bean.PagedResult;
import com.zlebank.zplatform.commons.utils.DateUtil;
import com.zlebank.zplatform.commons.utils.StringUtil;
import com.zlebank.zplatform.member.bean.MemberBalanceBean;
import com.zlebank.zplatform.member.bean.MemberBalanceDetailBean;
import com.zlebank.zplatform.member.bean.MemberBean;
import com.zlebank.zplatform.member.bean.MemberQuery;
import com.zlebank.zplatform.member.bean.enums.MemberType;
import com.zlebank.zplatform.member.exception.DataCheckFailedException;
import com.zlebank.zplatform.member.exception.GetAccountFailedException;
import com.zlebank.zplatform.member.service.MemberAccountService;
import com.zlebank.zplatform.member.service.MemberService;

/**
 * 会员账户相关服务
 *
 * @author Luxiaoshuai
 * @version
 * @date 2016年1月15日 下午2:52:31
 * @since 
 */
@Service
public class MemberAccountServiceImpl implements MemberAccountService {
    
    private Log log = LogFactory.getLog(MemberOperationServiceImpl.class);

    @Autowired
    private BusiAcctService busiAcctService;
    @Autowired
    private AccountService accountService;

    @Autowired
    private MemberService memberService;
    
    
    /**
     * 查询余额【通过会员号查询】
     * @param memberType
     * @param member
     * @return MemberBalanceBean 如果失败抛出异常
     * @throws DataCheckFailedException 
     * @throws GetAccountFailedException 
     */
    @Override
    public MemberBalanceBean queryBalance(MemberType memberType, MemberBean member) throws DataCheckFailedException, GetAccountFailedException {
        if(log.isDebugEnabled()){
            log.debug("参数1："+JSONObject.fromObject(memberType));
            log.debug("参数2："+JSONObject.fromObject(member));
        }
        if (StringUtil.isEmpty(member.getMemberId())) {
            throw new DataCheckFailedException("会员号不可为空");
        }
        try {
            long acctId = busiAcctService.getAccountId(Usage.BASICPAY, member.getMemberId());
            Account accountBalanceById = accountService.getAccountBalanceById(acctId);
            MemberBalanceBean mbb = new MemberBalanceBean();
            mbb.setBalance(accountBalanceById.getBalance().getAmount());
            return mbb;
        } catch (AbstractBusiAcctException e) {
            log.error(e.getMessage(), e);
            throw new GetAccountFailedException(e.getMessage());
        }
    }

    /**
     * 查询收支明细【通过会员号查询】
     * @param memberType
     * @param member
     * @param page
     * @param pageSize
     * @return
     * @throws GetAccountFailedException 
     */
    @Override
    public PagedResult<MemberBalanceDetailBean> queryBalanceDetail(MemberType memberType,
            MemberBean member,
            int page,
            int pageSize) throws GetAccountFailedException {
        // 返回值准备
        List<MemberBalanceDetailBean> rtnList;
        long rtnCount;
        MemberQuery queryCondition = new MemberQuery();
        // 取业务账户
        String busiCode;
        try {
            busiCode = busiAcctService.getBusiCodeByMemberId(Usage.BASICPAY, member.getMemberId());
        } catch (AbstractBusiAcctException e) {
            log.error(e.getMessage(), e);
            throw new GetAccountFailedException(e.getMessage());
        }
        queryCondition.setAcctCode(busiCode);
        try {
            PagedResult<AccEntry> details =  memberService.getAccEntryByQuery(page, pageSize, queryCondition);
            List<AccEntry> pagedResult = details.getPagedResult();
            rtnList = new ArrayList<MemberBalanceDetailBean>();
            rtnCount = details.getTotal();
            for (AccEntry acc:pagedResult) {
                MemberBalanceDetailBean bean = new MemberBalanceDetailBean();
                bean.setBudgetType(CRDRType.CR == acc.getCrdr() ? "00" : "01");
                bean.setTxnAmt(acc.getAmount().getAmount());
                bean.setTxnTime(DateUtil.formatDateTime("yyyy-MM-dd HH:mm:ss", acc.getInTime()));
                rtnList.add(bean);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new GetAccountFailedException(e.getMessage());
        }
        PagedResult<MemberBalanceDetailBean> rtn = new DefaultPageResult<MemberBalanceDetailBean>(rtnList, rtnCount);
        return rtn;
    }
}
