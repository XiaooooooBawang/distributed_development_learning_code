package cn.xbw.account.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * TCC接口实现 account-service 的全局事务。TCC可以和AT一起用
 */
@LocalTCC //声明TCC接口
public interface AccountTCCService {

    /**
     * try 资源锁定
     *
     * BusinessActionContextParameter 注解能把参数放到上下文对象中供confirm和cancel使用
     *
     * @param userId
     * @param money
     */
    @TwoPhaseBusinessAction(name = "deduct", commitMethod = "confirm", rollbackMethod = "cancel")
    void deduct(@BusinessActionContextParameter(paramName = "userId") String userId,
                @BusinessActionContextParameter(paramName = "money") int money);

    /**
     * confirm 提交
     *
     * @param ctx
     * @return
     */
    boolean confirm(BusinessActionContext ctx);

    /**
     * cancel 回滚
     *
     * @param ctx
     * @return
     */
    boolean cancel(BusinessActionContext ctx);
}
