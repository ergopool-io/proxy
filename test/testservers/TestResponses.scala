package testservers

object TestResponses {
  var info: String =
    """
      |{
      |  "currentTime": 1581174342468,
      |  "name": "ergo-node",
      |  "stateType": "utxo",
      |  "difficulty": 5356060672,
      |  "bestFullHeaderId": "f9f379dc55d6244cd1286dbbf1eaaa6af2417537d96f2a537a133749faf46852",
      |  "bestHeaderId": "f9f379dc55d6244cd1286dbbf1eaaa6af2417537d96f2a537a133749faf46852",
      |  "peersCount": 27,
      |  "unconfirmedCount": 1,
      |  "appVersion": "3.1.5",
      |  "stateRoot": "db664e211cfb75c35be0ef2d13caef105deffb9eb4f670ba14aa773b8155167214",
      |  "genesisBlockId": "f654fccd73b388177a7363788296c900475590e08cf8e945beb721064ebc3658",
      |  "previousFullHeaderId": "7bbae421973cdf36298f204c29d191c7d9afb1c7224480e4d112a31c33bd96e3",
      |  "fullHeight": 101043,
      |  "headersHeight": 101043,
      |  "stateVersion": "f9f379dc55d6244cd1286dbbf1eaaa6af2417537d96f2a537a133749faf46852",
      |  "fullBlocksScore": 136562532380672,
      |  "launchTime": 1580989674853,
      |  "headersScore": 136562532380672,
      |  "parameters": {
      |    "outputCost": 100,
      |    "tokenAccessCost": 100,
      |    "maxBlockCost": 1184298,
      |    "height": 100352,
      |    "maxBlockSize": 524288,
      |    "dataInputCost": 100,
      |    "blockVersion": 1,
      |    "inputCost": 2000,
      |    "storageFeeFactor": 1250000,
      |    "minValuePerByte": 360
      |  },
      |  "isMining": true
      |}
      |""".stripMargin

  var last10blocks: String =
    """
      |[
      |  {
      |    "extensionId": "e6e27cb51e3966e569c623a8e4bdf5ce422c8c266e4601d3688d0284c7d546fd",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173456937,
      |    "size": 281,
      |    "stateRoot": "88e1f92a99d5764637ce60dcc1a96f7f7b580867152f3496bde63d90f9d64dee14",
      |    "height": 101034,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "6608f7a8f7894a429b0dc6c108d4fcb48d80cbd04261c88d78fc1af48b44c4e1",
      |    "adProofsRoot": "48215d5e7f413a459ee786e44904e34ea789932071355b5735e261351ecf44a9",
      |    "transactionsRoot": "c5527b0a2a6f22e61aae6c5ce66b1d2a4b3e25546339738e634a3751770f482e",
      |    "extensionHash": "c0523ffd185a35a854f3b91f33f65f31e5c6d22b877c606a98ba885226ac241e",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "02fc7331d59a0ba325115ca980d448e561af67351749502cff952302abc850a515",
      |      "n": "000006feff5dc042",
      |      "d": 4.2101485721988647e+65
      |    },
      |    "adProofsId": "5ef83575f3f567f056a2ae8cae152bd9d672613abb971b55220d11ee755e10e0",
      |    "transactionsId": "0eda26ce74bb49a97d417b231cd039a2932b1877f5e1a4ca8bbdc2923d61aa59",
      |    "parentId": "c772c81350a147d42dddb57bdc1d52346c6b96ee994d4e67d93ea9a79a285639"
      |  },
      |  {
      |    "extensionId": "cb92cd8a35641d7b09b71518746d1126df631e49c1a44be70e48cd1b8ddac2b2",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173701080,
      |    "size": 281,
      |    "stateRoot": "5881535241edf2e8fd9c4562ddb465df1920a1a861f15083735320982184d3f114",
      |    "height": 101035,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "a4f893a2f743a571ef3871fdfbcd7f44d5bf11709decd431c6105e7bea708342",
      |    "adProofsRoot": "f0d9b508d4254a65391c7b35dd3c7d6a888426e151d11ae23026c615bd10aba9",
      |    "transactionsRoot": "4c9161a74a9e7fc814a7fcfaa60a2ef62057159ae5b8476f7332ef78253f979f",
      |    "extensionHash": "17f021b1cb257d3c8585cd62f5994aad5fb1f98629d2387ca0ac6f064c10fda7",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "024c24d7b6242101756b66f05130a0f66fbc18705212146cc2204c8aca007bd79b",
      |      "n": "000006ff761f9528",
      |      "d": 1.5810869645717022e+67
      |    },
      |    "adProofsId": "382166f98ca703a698d8a9aeaea1889615597ea17db83ee876c1a0a39336df92",
      |    "transactionsId": "206fbf6a9a9050857892fc395a80fc788a5c20e28675faccf6942395bd2fa9ad",
      |    "parentId": "6608f7a8f7894a429b0dc6c108d4fcb48d80cbd04261c88d78fc1af48b44c4e1"
      |  },
      |  {
      |    "extensionId": "82643c3397d1b39c503f7d9a1951627f0f645033ee624f89f93293c13d48babc",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173813416,
      |    "size": 282,
      |    "stateRoot": "d06a75b2b65b2dc3325896f2a451b2470858c2857b4a1275aef2b265b3937b2914",
      |    "height": 101036,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "6ceb9a1eccdfdbb507065792322b63160d08e9cf2c8bc141859c35797cad8c22",
      |    "adProofsRoot": "44e64f5d24e520811166230ec1a8f3eb4ba6fdbabe7dd8786d14a0723e561c19",
      |    "transactionsRoot": "a53f47f4de7b3945cc7251ce4c0f4e17fe8ff63f8cd24dbf4c172eef73fa3f5c",
      |    "extensionHash": "17f021b1cb257d3c8585cd62f5994aad5fb1f98629d2387ca0ac6f064c10fda7",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "03c55e4517e079b3cc649b7b3248bce105e5be15955b6894277cfe857e387f0dbb",
      |      "n": "000006ffa3742add",
      |      "d": 1.5837523977635488e+67
      |    },
      |    "adProofsId": "ea422ce764e5148a6abcb1205061b68193c357b795afaeda5e901c6f95101e67",
      |    "transactionsId": "ecf9eb56b63a87286ca06cedb4f19c2c64b4d70971f446f913ee3f526cfbf859",
      |    "parentId": "a4f893a2f743a571ef3871fdfbcd7f44d5bf11709decd431c6105e7bea708342"
      |  },
      |  {
      |    "extensionId": "8e71891db221de425ef822cc3259893c7977ac4b387ae553f58bfa3aa65ada38",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173857618,
      |    "size": 282,
      |    "stateRoot": "2db9b976d62f2ee2e18bcc8a1f3963527e3ec82b192a909aa07a3efff0d342c414",
      |    "height": 101037,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "e24e6710d0afd25fc2a8ebce4579de1d309c853103b9747e53261d38fe484f7c",
      |    "adProofsRoot": "c3834346a16a5204dbd65255a0ea85b845ac6a2d97bd4c8b206639d138594380",
      |    "transactionsRoot": "361afe4bf8bca6ebca02d6ae18e8d4735cc5512f93c04b76bfae766e25737cd4",
      |    "extensionHash": "17f021b1cb257d3c8585cd62f5994aad5fb1f98629d2387ca0ac6f064c10fda7",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "035dec213f2e41f5370caadf6ea4f2654956298dcbb2852d0dad755d504aeb8016",
      |      "n": "000006ffa580348d",
      |      "d": 3.2567469398139614e+66
      |    },
      |    "adProofsId": "28e1db42fb941cacd16ec9f7a472f9b0ebfc82b29993ac95cda155edf2db05a6",
      |    "transactionsId": "268196fddfe3965071764126fd273027612456669f8b0de57e7aaed748881303",
      |    "parentId": "6ceb9a1eccdfdbb507065792322b63160d08e9cf2c8bc141859c35797cad8c22"
      |  },
      |  {
      |    "extensionId": "191a1887273c584e934f494c8fa674cf18310b3c57f1be2227c7c8bd600724b9",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173864437,
      |    "size": 282,
      |    "stateRoot": "8afccf96cad30825154187db1213c66ebe29fe6233f7cded7344f222fcdacb6714",
      |    "height": 101038,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "fbc746926c95b1e4b033c85aecafe96b91c0544a30ed2b47955f17aaade10aea",
      |    "adProofsRoot": "edc01e842deb7e6cf8fc13e773d629eecdf4c3429d042c61d178978644b303fc",
      |    "transactionsRoot": "4697d68bd894701fd2d38dc5db48f6e0aae8e859e042e6651edfac37fc464e5b",
      |    "extensionHash": "f0b9c02dc4dbd806018e42ec2fe550e6405b561777816c0781bd4ed69b3f078f",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "0255a3c82d1e01614f702eff25dff963eb387306c620c6805cd82262dea5ab7394",
      |      "n": "000000462d2daaf7",
      |      "d": 2.0989802357259208e+67
      |    },
      |    "adProofsId": "d08eb50cc72e2cf66954c8bf448c77f8070d016b54531fc0f629e23207fc81e5",
      |    "transactionsId": "cb13b3828d447b6d71ba598fac7ff117651130f2d467e5c471c8a391869146f1",
      |    "parentId": "e24e6710d0afd25fc2a8ebce4579de1d309c853103b9747e53261d38fe484f7c"
      |  },
      |  {
      |    "extensionId": "70fccb8f8cd631a51445af9cafd632b053f2583bfe4eb642fe1e0bb4101434bd",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173906904,
      |    "size": 282,
      |    "stateRoot": "9083ac31c83da692a952ecf034e7760b44f31f75223e5040a618ea30e207cdc814",
      |    "height": 101039,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "25ab1bb644db134d0aaccca675a5e3ce07b019fb59ea8b2c52e6cfb7b40105a0",
      |    "adProofsRoot": "901e2521af89e846df46c4fa45fbd1ccc449ecb354e81e8843ad33e012899fe8",
      |    "transactionsRoot": "1cddeb5eadb4ce6b95929c851528d4071dc4abd32b29d6c3478410cfd1c70147",
      |    "extensionHash": "f0b9c02dc4dbd806018e42ec2fe550e6405b561777816c0781bd4ed69b3f078f",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "02c96bdb33f8283c4dc71363f1728a81a818d96d8478d05132dfa4b033eeb6d01a",
      |      "n": "00000046395eb344",
      |      "d": 8.32646226523466e+66
      |    },
      |    "adProofsId": "b769ddca88d10deffaa73b02d09dad599de4d12e252ce383d0dfa1dcc6c37462",
      |    "transactionsId": "04913c76be07bed69780226e01ee94ddbefdb933c7dfda7cfb4358f9213a185b",
      |    "parentId": "fbc746926c95b1e4b033c85aecafe96b91c0544a30ed2b47955f17aaade10aea"
      |  },
      |  {
      |    "extensionId": "962581944f3c92fe8716a3db5bbfa966e6c023e89d84b24983907f80dcec7f84",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581173915176,
      |    "size": 281,
      |    "stateRoot": "2de8f70067e4a3e07fa7aa8416b594f891661ea708707d67271dc460eac5fd1114",
      |    "height": 101040,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "6ad55cb8827e2ab8bc9cc562448f87d8c0ac14d2dfce829ab717cde775ba349c",
      |    "adProofsRoot": "5565951e197e64d3c53ab7e5a283851b45823a3e0c903905ae11ace47e8b4341",
      |    "transactionsRoot": "6053fa71a58743c4a7d1ef03bdf55954ce8c8829207f56c8a4b8d2e088342c18",
      |    "extensionHash": "5d2795a09c85eafd5c1344358c09a6c4b2cc22a79bda70711f48f01d719059b4",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "0239ef8099f59fe0d5aaed22570083eb15500051370c2ecb6784b456c9dc58414e",
      |      "n": "00000046f7935136",
      |      "d": 1.6586681746851647e+67
      |    },
      |    "adProofsId": "42d6ebedcda48007779b9a45650ab76892c81f5202be0b5f4ef4478b90c70853",
      |    "transactionsId": "b24d6a3f52e9a734d13971a3a2d540cd4bba9dabd0e5e89eacdd3624e77a0282",
      |    "parentId": "25ab1bb644db134d0aaccca675a5e3ce07b019fb59ea8b2c52e6cfb7b40105a0"
      |  },
      |  {
      |    "extensionId": "2350b6c86071d9a3961922463e1a4c47b8d345e98bc99a20659fe5ee3b71054a",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581174033619,
      |    "size": 281,
      |    "stateRoot": "0ddf3725efdfd1db0995535fa1f77ae86a6a802d42eb25dfbc6f5aec0bd7b2ea14",
      |    "height": 101041,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "a0ec79d44e41ddd5efc3a3881c0170ccb751a842d13748a750c64fd2b536b839",
      |    "adProofsRoot": "78bceea0917df02edf027092adae3dad613dd78c6c960b4a06442e42f8d394d6",
      |    "transactionsRoot": "21c56434cd61285393b400a184c9b53330bc73619c0d8d0933e17dc3004a3949",
      |    "extensionHash": "5d2795a09c85eafd5c1344358c09a6c4b2cc22a79bda70711f48f01d719059b4",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "03005e924ae07e6f5d05cc2e4a301502e193cffd164ae65c5fff24d2607e263d03",
      |      "n": "000007009817c02c",
      |      "d": 1.9275822738141093e+66
      |    },
      |    "adProofsId": "90bf1b81da48cd62f62f5df5465bb2919c0bce75af757b3875841bb09cc27842",
      |    "transactionsId": "41b7bd804364ab7bc7854ada99f00f38bc3e0362d77158343e1a8081b53d4f99",
      |    "parentId": "6ad55cb8827e2ab8bc9cc562448f87d8c0ac14d2dfce829ab717cde775ba349c"
      |  },
      |  {
      |    "extensionId": "b3f9eb09a02846cbe45e5823c478db192eaebd1e1261a35d9ea8eb7295371aa6",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581174097414,
      |    "size": 282,
      |    "stateRoot": "9027e871e900334d0992e288977c7698c29a49c0b3a1acbc450c222f4b23620e14",
      |    "height": 101042,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "7bbae421973cdf36298f204c29d191c7d9afb1c7224480e4d112a31c33bd96e3",
      |    "adProofsRoot": "99d9ce2590274b8d17f41e7c308be5303562ab4d6fa80ae5de57baf65f1e59fb",
      |    "transactionsRoot": "c3e3ddd77a6b079fbab8b8b3ac52157dd53df081fde79b27686022c35a109bf8",
      |    "extensionHash": "12a95ff91173bfdbe585f86caa540a471a85fef1479c3db0f6a6eb79c97b8720",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "035e875384018314d4aee0408ed36e6591ef93e354703611ad0f2554cd64f60c52",
      |      "n": "0000004837a91a2e",
      |      "d": 8.323518512103463e+66
      |    },
      |    "adProofsId": "fe1663a83c5d4c27f2886bf5081052c78450a8128d99834765d1f8c95878f35b",
      |    "transactionsId": "e842fbdaee360842efe0691b531729091a84b6e5e68a8749a52f4e554f4bdbfe",
      |    "parentId": "a0ec79d44e41ddd5efc3a3881c0170ccb751a842d13748a750c64fd2b536b839"
      |  },
      |  {
      |    "extensionId": "a68d7841558aba9098c2a2879d222cfd72921e9f1cafa717f2a010302778a176",
      |    "difficulty": "5356060672",
      |    "votes": "040000",
      |    "timestamp": 1581174232718,
      |    "size": 282,
      |    "stateRoot": "db664e211cfb75c35be0ef2d13caef105deffb9eb4f670ba14aa773b8155167214",
      |    "height": 101043,
      |    "nBits": 83967807,
      |    "version": 1,
      |    "id": "f9f379dc55d6244cd1286dbbf1eaaa6af2417537d96f2a537a133749faf46852",
      |    "adProofsRoot": "28604a6410d794578c3a249a17e152c055d8833eb73377f841018f49a0d8bd56",
      |    "transactionsRoot": "75d65a7518fd38fd75a738678cf678cc06b9de76147629663f0b67b6fc9d4400",
      |    "extensionHash": "0f06173ce518e11f0436becc13af9876adfa2ca0be371111d3edc99a8d0ae8f9",
      |    "powSolutions": {
      |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
      |      "w": "037c410d0f31d549a7731c46406e3db72b4c297a9b64a9c26876d2fde0f8048f6b",
      |      "n": "0000070127c90413",
      |      "d": 2.0454294664063203e+67
      |    },
      |    "adProofsId": "d67db3fb50aa6b7370c1c0aec0701e2ff840ef6e8e9656bae6702531dd9e24da",
      |    "transactionsId": "3ce8e86da3864fe6afcc982b73f784994be2740c9afaea0d137e6b298e9420c8",
      |    "parentId": "7bbae421973cdf36298f204c29d191c7d9afb1c7224480e4d112a31c33bd96e3"
      |  }
      |]
      |""".stripMargin

  var unspentBoxes: String =
    """
      |[
      |   {
      |    "confirmationsNum": 32861,
      |    "address": "5Hg4a36kRJxyZpQBh4g5ConDLfFZFNAu2UvhuMydaLcqQwg5CySs1ptD3aFMHHHie5eZ6cNwW8JWTTduodU5U4eAVvRkV3QJVExpUZaxzv5grXsx8At4yAcyvtNb1vYQtf5Zo68qAGKp4sTDYqEV1M2kiH7kdBCzHzLYnxCMEYJJ4qA45MSqKQV",
      |    "creationTransaction": "6c250881ca75431941cc07d3e5fd71404ad70703bcd6eecf5a0ee9e475dfc544",
      |    "applicationId": 1,
      |    "certain": true,
      |    "onchain": true,
      |    "creationOutIndex": 2,
      |    "spendingTransaction": null,
      |    "box": {
      |      "boxId": "eac5cb6610e94768194b7f35a6798e3215c9ebaad046d4a9a41d86e04b47169a",
      |      "value": 65001000000,
      |      "ergoTree": "100308cd03a11b4ec6be65bf20a7bf3d42d19c1755e53e817a8ccec7afc28f8be5f39462e808cd0327631f32a33e18481d13f13ba15326e9401220b815550ae6a98371525b3765c208cd03ef05027bc12a3d57d623a2240074166ac26d642830df25b437b139b3a0ad07f2eb02ea02d193d0cddb6906db6503fed0730073017302",
      |      "creationHeight": 103262,
      |      "assets": [],
      |      "additionalRegisters": {},
      |      "transactionId": "331ed3109e206ec6f8fe05783ce3e4c4cf78f256366bf84c8c92f302ecfd6dd1",
      |      "index": 0
      |    },
      |    "spendingHeight": null,
      |    "inclusionHeight": 183262,
      |    "spent": false
      |  },
      |  {
      |    "confirmationsNum": 32859,
      |    "address": "3WxrJNDau1ootpbqG6UFAZJ6CZL7SDuzW2tpktdYX4yDRxaejWzr",
      |    "creationTransaction": "65138e6b10e89001a4287d9cb44be47a829f556b757487e36711f14a412dd4d1",
      |    "applicationId": 1,
      |    "certain": true,
      |    "onchain": true,
      |    "creationOutIndex": 0,
      |    "spendingTransaction": null,
      |    "box": {
      |       "boxId": "cddfd0dcfaa448805af56bfba14ac6efbc5af0e18fae077f8e3b7422cf4612bf",
      |       "value": 4000000000,
      |       "ergoTree": "10030703dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c92108cd0327631f32a33e18481d13f13ba15326e9401220b815550ae6a98371525b3765c208cd03dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c921eb02ea02d193db6906db6503fe730073017302",
      |       "creationHeight": 101858,
      |       "assets": [],
      |       "additionalRegisters": {},
      |       "transactionId": "cea762459e63ca433766cff19a10dba484ff1249545ba2cb25c60391fcc0bef6",
      |       "index": 0
      |    },
      |    "spendingHeight": null,
      |    "inclusionHeight": 68184,
      |    "spent": false
      |  },
      |  {
      |    "confirmationsNum": 31877,
      |    "address": "3WxrJNDau1ootpbqG6UFAZJ6CZL7SDuzW2tpktdYX4yDRxaejWzr",
      |    "creationTransaction": "ae04281f390fe3027e140745f33e2016b45a5bf5353651284735c97204928f51",
      |    "applicationId": 1,
      |    "certain": true,
      |    "onchain": true,
      |    "creationOutIndex": 0,
      |    "spendingTransaction": null,
      |    "box": {
      |       "boxId": "fcd20bd7f52aaf4ce193a0dc3b4132ac140af7e26c5bc30106ba57eacd95ae30",
      |       "value": 750000000,
      |       "ergoTree": "10030703dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c92108cd0327631f32a33e18481d13f13ba15326e9401220b815550ae6a98371525b3765c208cd03dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c921eb02ea02d193db6906db6503fe730073017302",
      |       "creationHeight": 101858,
      |       "assets": [],
      |       "additionalRegisters": {},
      |       "transactionId": "30b8f854f4a958758bcbb2f56721d46e6b08e3b5d6924a0184e6d80f0c1f5aa1",
      |       "index": 0
      |    },
      |    "spendingHeight": null,
      |    "inclusionHeight": 69166,
      |    "spent": false
      |  },
      |  {
      |    "confirmationsNum": 31877,
      |    "address": "3WxrJNDau1ootpbqG6UFAZJ6CZL7SDuzW2tpktdYX4yDRxaejWzr",
      |    "creationTransaction": "ae04281f390fe3027e140745f33e2016b45a5bf5353651284735c97204928f51",
      |    "applicationId": 1,
      |    "certain": true,
      |    "onchain": true,
      |    "creationOutIndex": 0,
      |    "spendingTransaction": null,
      |    "box": {
      |       "boxId": "c57b8a364052e9ddb38eead021945ae1e83195258a7ed376421b975b6d0dec36",
      |       "value": 1000000000,
      |       "ergoTree": "10030703dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c92108cd0327631f32a33e18481d13f13ba15326e9401220b815550ae6a98371525b3765c208cd03dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c921eb02ea02d193db6906db6503fe730073017302",
      |       "creationHeight": 101858,
      |       "assets": [],
      |       "additionalRegisters": {},
      |       "transactionId": "afeb1900b0b1007ba11830c016007ac335a21936c208a8b0fb54c99bb979b308",
      |       "index": 0
      |    },
      |    "spendingHeight": null,
      |    "inclusionHeight": 69166,
      |    "spent": false
      |  }
      |]
      |""".stripMargin
}
