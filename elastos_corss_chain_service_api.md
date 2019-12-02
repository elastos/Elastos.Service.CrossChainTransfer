# ELASTOS BAAS APIs

* 通信结构：
    success --> {"state":0, "data":{json_data}}
    error   --> {"state":!=0, "message":"错误提示信息"}

## 快速转账
1. 【用户】获取转账链信息（链列表，汇率，转账手续费）
HTTP: GET
URL : /api/1/ela_exchange/chainlist
return:
    成功：{
        "state":0,
        "data": {
            [
                {
                    "chain_name":"ela",
                    "chain_id":1
                },
                {
                    "chain_name":"ela-did",
                    "chain_id":2
                }
            ]
        }
    }
    失败:{"state":400, "message":"Err msg"}

1. 【用户】获取链间转账信息 
HTTP: GET
URL : /api/1/ela_exchange/rateinfo?src={1}&dst={2}
return:
    成功：{
        "state":0,
        "data": {
            {
                "src_chain_id":1,
                "src_chain_name":"ela",
                "dst_chain_id":2,
                "dst_chain_name":"ela-did",
                "rate":1,
                "fee_rate" : 0.001,
                "threshold_min":1.0,
                "threshold_max":100.0
            }
        }
    }
    失败:{"state":400, "message":"Err msg"}

1. 【用户】生成订单(输入链与收款链+地址)
HTTP: POST
URL : /api/1/ela_exchange/generator
data: {
    "src_chain_id":1,
    "back_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZbac",
    "dst_chain_id":2,
    "dst_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc",
    "user_did":"iUJpfZMxs3p1AdiR8Mqv35PNyDvnjw5vr8"
}
return:
    成功：{
        "state":0,
        "data": {
            "exchange_id":"c24f8d4478e8517a96b33b35fbe1773e1",
            "src_chain_id":1,
            "src_chain_name":"ela main chain",
            "src_chain_addr":"CUDckybfP8X3odpuQSrJye3hxG5YTYvHc8"
            }
    }
    失败:{"state":400, "message":"Err msg"}

1. 【用户】查询转账信息(状态)
HTTP: GET
URL : /api/1/ela_exchange/{did}/tx/{exchange_id}
return:
    成功：{
        "state":0,
        "data": {
            "exchange_id":1,
            "src_chain_id":1,
            "src_chain_addr":"CUDckybfP8X3odpuQSrJye3hxG5YTYvHc8"
            "src_chain_name":"ela main chain",
            "src_value":"1.999999"
            "dst_chain_id":2,
            "dst_chain_name":"did side chain",
            "dst_chain_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc"
            "dst_value":"1.999999"
            "type": "exchange",
            "state":"renewal_waiting",
            "txid":"60761b2854a31f20ebba854d5fbe40f637070c87b720c1520b5b7c3c4e082cd4",
            "create_time": 1564553525000
        }
    }
    失败:{"state":400, "message":"Err msg"}

    转账状态可能取值：
    ```
    "renewal_waiting"
    "renewal_timeout"
    "transferring"
    "transfer_finish"
    "transfer_failed"
    "backing"
    "back_finish"
    "back_failed"
    "direct_transferring"
    "direct_transferring_wait_gather"
    "direct_transfer_finish"
    "direct_transfer_failed"
    ```

1. 【用户】查询转账明细(状态)
HTTP: GET
URL : /api/1/ela_exchange/{did}/txs
return:
    成功：{
        "state":0,
        "data": [{
            "exchange_id":1,
            "src_chain_id":1,
            "src_chain_addr":"CUDckybfP8X3odpuQSrJye3hxG5YTYvHc8"
            "src_chain_name":"ela main chain",
            "src_value":"1.999999"
            "dst_chain_id":2,
            "dst_chain_name":"did side chain",
            "dst_chain_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc"
            "dst_value":"1.999999"
            "type": "exchange",
            "state":"renewal_waiting",
            "txid":"60761b2854a31f20ebba854d5fbe40f637070c87b720c1520b5b7c3c4e082cd4",
            "create_time": 1564553525000
        }]
    }
    失败:{"state":400, "message":"Err msg"}

==================================================
1. 【管理】获取平台钱包信息(各个链充值钱包与转账钱包列表)
HTTP: GET
URL : /api/1/ela_exchange/manage/value
return:
    成功：{
        "state":0,
        "data":
        {
        "deposit_address": [
            {
                "chain_id": 1,
                "value": 36.909959
            },
            {
                "chain_id": 2,
                "value": 0.1
            }
        ],
        "exchange_wallets": [
            {
                "chain_id": 1,
                "value": 20.0,
                "wallet_sum": 20
            },
            {
                "chain_id": 2,
                "value": 0.0,
                "wallet_sum": 20
            }
        ]
        }
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】获取充值地址
HTTP: GET
URL : /api/1/ela_exchange/manage/deposit
return:
    成功：{
        "state":0,
        "data":
                { 
                "address":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc",
                }
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】查询交易明细（分类:跨链转账(exchange)，管理充值(renewal)，链间自动平衡(balance)）
HTTP: GET
URL : /api/1/ela_exchange/manage/txdetail
return:
    成功：{
        "state":0,
        "data": [{
            "exchange_id":1,
            "src_chain_id":1,
            "src_chain_addr":"CUDckybfP8X3odpuQSrJye3hxG5YTYvHc8"
            "src_chain_name":"ela main chain",
            "src_value":"1.999999"
            "dst_chain_id":2,
            "dst_chain_name":"did side chain",
            "dst_chain_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc"
            "dst_value":"1.999999"
            "type": "exchange",
            "state":"renewal_waiting",
            "txid":"60761b2854a31f20ebba854d5fbe40f637070c87b720c1520b5b7c3c4e082cd4",
            "create_time": 1564553525000
        }]
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】失败交易再起启动转发
HTTP: GET
URL : /api/1/ela_exchange/manage/dealfailed
return:
    成功：{
        "state":0,
        "data": [{
            "exchange_id":1,
            "src_chain_id":1,
            "src_chain_addr":"CUDckybfP8X3odpuQSrJye3hxG5YTYvHc8"
            "src_chain_name":"ela main chain",
            "src_value":"1.999999"
            "dst_chain_id":2,
            "dst_chain_name":"did side chain",
            "dst_chain_addr":"EbbQhu2riAhrcQP7dUJYakARCrpNDWZsrc"
            "dst_value":"1.999999"
            "type": "exchange",
            "state":"renewal_waiting",
            "txid":"60761b2854a31f20ebba854d5fbe40f637070c87b720c1520b5b7c3c4e082cd4",
            "create_time": 1564553525000
        }]
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】关闭所有定时任务
HTTP: GET
URL : /api/1/ela_exchange/manage/stoptask
return:
    成功：{
        "state":0
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】开启所有定时任务
HTTP: GET
URL : /api/1/ela_exchange/manage/starttask
return:
    成功：{
        "state":0
    }
    失败:{"state":400, "message":"Err msg"}

1. 【管理】归集所有资金到主链充值地址
HTTP: GET
URL : /api/1/ela_exchange/manage/gather
return:
    成功：{
        "state":0,
        "data": {
            "value": 13.900392
        },
    }
    失败:{"state":400, "message":"Err msg"}

## 跨链转账服务基本设计
1. 跨链转账钱包服务基础架构
    * 由于涉及多个链互转以及用户转账可追溯，所以每个链都有至少一个钱包（下文称充值钱包）生成地址用于获取用户输入（用户转账到我们系统）
    * 为了钱包管理清晰与高并发，转出是专门使用一批钱包（每条链一个，下文称为转账钱包）来完成
    * 在主链上有一个汇总钱包地址，用于管理员充值到系统，以及收集所有用户转入资金，并且发送资金到余额不足的转账钱包。

2. 用户转账主流程：
    1. 用户用大象钱包登录，系统获取到用户did，作为用户标识。
    2. 用户选择输入输出链，填写输出地址与失败退回地址
    3. 充值钱包在用户输入链上分配新地址给用户作为用户输入地址。
    4. 用户转账到充值地址
    5. 系统在用户目的链上从转账钱包中的一个地址发送资金到用户目的地址

3. 并发处理
    * 为了提高并发量，转账钱包会同时有多个地址拥有余额，用于用户转账。
    * 为了防止大量资金转账导致转账钱包被短时间耗尽丧失并发能力，我们设定单次转账限额，并且每次转账最多使用一个地址内的资金。
    * 用户转入超过限额资金，将直接进行跨链转账.
    * 如果并发过高，导致所有用于快速转账的钱包地址都被耗尽，讲直接进行跨链转账。

4. 资金归集与循环利用
    * 由于目前各个侧链是无法直接相互转账，只能主链侧链互转，所以资金归集设计上考虑基于主链的汇总钱包地址进行管理。
    * 管理员首先充值足够数量资金到汇总钱包，系统根据各个链的钱包地址数量以及每个地址所需要基础资金进行分配。
    * 系统定期扫描已经成功的转账记录获取可以搜集的用户充值钱包地址，将其中资金汇聚到汇总钱包。
    * 定期检查转账钱包余额，小于最低阀值，由汇总钱包进行充值。

## 页面规划
1. 首页：检测用户是否已经用钱包登录（页面session是否保存did信息），如果未登录，跳转到钱包辅助登录服务，用户钱包登录后跳转回本页。如果用户已经登录（获取到用户did）则跳转到用户转账页面
2. 用户转账页面，显示用户可以选择的输入链（api获取），输出链，输入输出用户单选确定，显示这两个链的汇率信息（api获取），用户输入转账目的地址（收钱地址），退款地址，确认开始转账后进入用户转账中页面。
3. 用户转账中页面显示用户充值地址，并且显示用户转账状态信息（api轮训获取），用户成功充值转账状态信息需要改变，当系统返回转账成功后跳转到用户转账成功页面
4. 用户转账成功页面。提示用户等待30min，如果还未收到款项联系elastos。
5. 用户历史转账信息页面，获取到用户的转账信息列表（api）并且显示。
* 功能可参考https://swft.elabank.net
