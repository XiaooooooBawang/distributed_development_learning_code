package cn.xbw.account.web;

import cn.xbw.account.service.AccountTCCService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用TCC做account业务的全局事务
 *
 * @author 虎哥
 */
@RestController
@RequestMapping("account")
public class AccountController {

    @Resource
    private AccountTCCService accountTCCService;

    @PutMapping("/{userId}/{money}")
    public ResponseEntity<Void> deduct(@PathVariable("userId") String userId, @PathVariable("money") Integer money){
        accountTCCService.deduct(userId, money);
        return ResponseEntity.noContent().build();
    }
}
