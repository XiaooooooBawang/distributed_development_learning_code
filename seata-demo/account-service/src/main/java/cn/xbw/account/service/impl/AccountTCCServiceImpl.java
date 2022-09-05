package cn.xbw.account.service.impl;

import cn.xbw.account.entity.AccountFreeze;
import cn.xbw.account.mapper.AccountFreezeMapper;
import cn.xbw.account.mapper.AccountMapper;
import cn.xbw.account.service.AccountTCCService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service
public class AccountTCCServiceImpl implements AccountTCCService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private AccountFreezeMapper accountFreezeMapper;

    @Override
    @Transactional
    public void deduct(String userId, int money) {
        // 0.获取事务id
        String xid = RootContext.getXID();
        // 1.防止业务悬挂，同时也相当于做了幂等判断
        AccountFreeze oldFreeze = accountFreezeMapper.selectById(xid);
        if (oldFreeze != null) {
            //cancel执行过，要拒绝业务
            return;
        }
        // 2.扣减可用余额，因为库表中该字段设计为无符号，非负，所以我们不需要自己判断减到小于0，直接减
        accountMapper.deduct(userId, money);
        // 3.记录冻结金额，事务状态
        AccountFreeze freeze = new AccountFreeze();
        freeze.setXid(xid);
        freeze.setUserId(userId);
        freeze.setFreezeMoney(money);
        freeze.setState(AccountFreeze.State.TRY);
        // 4.插入数据
        accountFreezeMapper.insert(freeze);

    }

    @Override
    public boolean confirm(BusinessActionContext ctx) {
        // 1.获取事务id
        String xid = ctx.getXid();
        // 2.根据id删除冻结记录
        int cnt = accountFreezeMapper.deleteById(xid);
        return cnt == 1;
    }

    @Override
    public boolean cancel(BusinessActionContext ctx) {
        String xid = ctx.getXid();
        String userId = ctx.getActionContext("userId").toString();
        int money =(Integer) ctx.getActionContext("money");

        // 0.查询冻结记录
        AccountFreeze freeze = accountFreezeMapper.selectById(xid);
        // 1.如果为空，做空回滚
        if (freeze == null) {
            AccountFreeze freeze1 = new AccountFreeze();
            freeze1.setXid(xid);
            freeze1.setUserId(userId);
            freeze1.setFreezeMoney(money);
            freeze1.setState(AccountFreeze.State.CANCEL);
            accountFreezeMapper.insert(freeze1);
            return true;
        }

        // 2.幂等判断
        if (freeze.getState() == AccountFreeze.State.CANCEL) {
            //已经处理过一次cancel了，无须再处理
            return true;
        }

        // 3.恢复可用余额
        accountMapper.refund(freeze.getUserId(), freeze.getFreezeMoney());
        // 4.将冻结金额清零，状态改为CANCEL。该条数据不能删，因为要留着防止业务悬挂
        freeze.setFreezeMoney(0);
        freeze.setState(AccountFreeze.State.CANCEL);
        int cnt = accountFreezeMapper.updateById(freeze);
        return cnt == 1;
    }
}
