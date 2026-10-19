package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

/**
 * The path-following campaign may adjust arrow exits while retaining silhouettes.
 * Protect the exact guitar silhouette and unchanged introductory puzzles, and
 * retain the numeric level identities consumed by existing progress records.
 */
class LevelPreservationTest {
    @Test
    fun `path-following conversion preserves the exact guitar occupied mask`() {
        val level = GuitarLevel.level
        assertEquals(69, level.cols)
        assertEquals(69, level.rows)
        val occupied = level.arrows.flatMap { it.cells }.toSet()
        assertEquals(1465, occupied.size)
        val canonical = occupied.sortedWith(compareBy<com.amowpuzzle.aitricker.domain.model.Cell> { it.row }.thenBy { it.col })
            .joinToString("") { "${it.col},${it.row};" }
        assertEquals("The traced guitar silhouette changed",
            "f9f4736c86e17bfe77f37e15faaa02d5b3c377a90760985a7228e4db0f03e364", sha256(canonical))
    }

    @Test
    fun `the first five introductory puzzles retain their exact layouts`() {
        val expected = listOf(
            "94ca02de400000acc56cc2c073bb059a15c0586596edf4d13e7bb837c4e38dbb",
            "22d13d32cef5ac85fe67fd1276908a7926f809a4854a0e24ff8e234445e4cd59",
            "55888af33b929b9e5ed67c6c2c618826f304e1804076e55707ac3a29951af359",
            "d19b4f3f4f33f2ffa015ae157cd24ab285d6c9173dc3084d42ada1a819100270",
            "d65db47f7fa53b5f7573bea4af2a9808bc2f3a5991702599c1809b6f9ac6c338"
        )
        for (id in 1..5) assertEquals("Introductory level $id changed",
            expected[id - 1], geometryFingerprint(LevelCatalog.level(id)))
    }

    @Test
    fun `adding fifty expert shapes preserves every original campaign puzzle`() {
        // Captured from the compiled, solver-verified 50-level catalog before expansion.
        // Includes dimensions, ordered arrow IDs, every cell, exits, style and movement rule.
        val expected = listOf(
            "94ca02de400000acc56cc2c073bb059a15c0586596edf4d13e7bb837c4e38dbb",
            "22d13d32cef5ac85fe67fd1276908a7926f809a4854a0e24ff8e234445e4cd59",
            "55888af33b929b9e5ed67c6c2c618826f304e1804076e55707ac3a29951af359",
            "d19b4f3f4f33f2ffa015ae157cd24ab285d6c9173dc3084d42ada1a819100270",
            "d65db47f7fa53b5f7573bea4af2a9808bc2f3a5991702599c1809b6f9ac6c338",
            "fa15c2f9dd400701b1ffe8deeea512342624e8e1aabc1628073926b24b94c219",
            "7e8d016002af4414aef47eb86da7fb458b32ad653c996539aa5e9035109d738e",
            "08e8234914664aab02a5e90732ee4b00da58294141c27e33de8966ed7005ef96",
            "33b920a83369ffa9f0615b9c48a748d78820650d3724e4593ef3426bed9096cf",
            "ea700b6837fd4218eb4750caf1a84cc9f2bc9d67ea00ea565a48641e93adc794",
            "c5b8d093964cf92992a4f1b4294ada0f83d2fbfed4c1317a978d1b9a2882ec3f",
            "69af304533f3c3af67537b017a8a6b81a1a443a2c32d5428066eae3c71e35716",
            "f077fb1dbdd9486b5e5cf5da188047a482bae4cbe6694f7b292db0b1c977cad2",
            "7d4634ca8ee8673af41fb4299e2275d2e6104a3084d6125065cbfa07168a37b8",
            "ba41643fd70dcaeba5416dc7e165931fb1954f756e85f0716573f7ff7c896891",
            "3b21be69b68f0c47434d567f57a66cbde23cf3dadec80cba1283cc81ab6e2eb0",
            "9c937a281c888ab69dcd36d2f25ce32825640223376d1dac72211e7fe508c945",
            "1971da338d914ecd376285063362bebfeb9427a29e6db65c7c652d85080d8db9",
            "0305d2ce805ff47682104095fbd53e4aed0ad77e3fd8ca8bf7e34fbd391f3c8a",
            "d35639f0fe8e2a1d0d22eeccd492b63329080f7bf1dd6a4232caf9c9868f1d2a",
            "daf7fbd37aa9c40778656be4ee0a063308e04e4acb1ec937d471d9ab0b334538",
            "8d43fcbec75d3ce4d3908c6a187b5c26a6e326e4e465e5a031b9ebe3e2c18fae",
            "ce89bc0b69ae0bf937d8585ecca3a7302ec4a0a2fcc26d12d6ca370e411abc0e",
            "b8a4478af4f0a6a01db5bd596a64e98f624654f356bd546c1cd3ef612be3c4ec",
            "86e0e7b7bddf13a6244a211a9805a9735a26e525d496f60a16c91ccc18f83755",
            "14fd1bbf698948bf74f8fd6404c5acbe6954fc239066abe6deb0f6934e263291",
            "ffc002431b28c81948ab55020f9a9bed20d42a5b2b1e349ef637e6c52c416550",
            "fc25561c3c2856b07e353bee37801dfe2099de988758c5379203bc87c44fed78",
            "551e41caf2dda045f737677f20fe1159e0945097534c68b5239161d58cd3f408",
            "12ea216972969c1a3458dd006e32119b845bb2f4fdc6c6dfa7cef3a3fb69dfe0",
            "dedc67734081b7b5268cdc0c32d22ca3d6cd1687ee3e5137ef74db84592aeeb7",
            "5c0853869131aad4d3b579fe0252ad83c5bdec105a0802f4d8b067eddc780a18",
            "2ccf771b6dfcb2b33b4ea3efbc878683c6b741d68082a5a682b4691c1b9a57bc",
            "a09fa4f4141c640f35fb1a1d121752b67f815d2a40187fde182d705ffaeba3e0",
            "2882216961736e72b019e772361a54819c6853a71a057f9d45335c5021e4ffed",
            "6a97e6f2dc919cb8df9db8b203721bd6c7eacf90ad0b984a85e3086e1c913277",
            "fb8a193e69d57dcfa9dbd42c2204832d327259bb88fde425ac6df775a3a693dc",
            "0fb5008dbc6e43e748466255fedd74f14eeb23b6ece1c5adb3d7862f7aca2d95",
            "d384f4e3f0cdc4d74a6f8fa5d58eeebe910d31665cf45cc8b6f4baa44b476d66",
            "ff2b7c9a1e48574f7962e138e72a6ba75a5ef1ededd9765c515acc210272456e",
            "d519b0c3e449fa7492b3633f61b146845ec39d7850e02ff5f0cf8a7d7dd5e716",
            "0126ba6b2b8b241eaf26d4efaf53a1f494d73ca11b3e00860aeab45cbcdfe306",
            "e09ee8e01f8a3984ba5d5d627a46d220dbc7145a87b6aedb499a6b4966703f05",
            "1df28af99fe8c3904ceebba47b9ba92e56636f74f9d2ac764394072d6bc37c96",
            "f31320b1905151ccfdbfe6074770b3e7c04ce9ae418f75f1d780fdde40752b6a",
            "5d3b99cd6ee36a7837b5105fef318dec8056c871c927ea501e419815d645fb3a",
            "802906d63da26a8b690c3ee5f6edf7e2e86ef1d17c7752e0fa44178527a260fe",
            "07740a70e970a642f5604e29e20d749563f1f4ca90f2ca069ed3d47fec4e5452",
            "709cb0bef2ddc1323405ac88ca4e782afde6b9d2478e84b374116671581701d3",
            "b6f11eb34930aa0a88b3d88d7644fb451e542d1d75edc642fc03fa1642899b12"
        )
        for (id in 1..50) assertEquals("Existing level $id changed during expansion",
            expected[id - 1], geometryFingerprint(LevelCatalog.level(id)))
    }

    @Test
    fun `campaign identities and starting resources remain compatible with existing progress`() {
        assertEquals((1..1500).toList(), LevelCatalog.levelIds.map { LevelCatalog.level(it).id })
        for (id in LevelCatalog.levelIds) {
            val level = LevelCatalog.level(id)
            assertEquals("Level $id lives", 3, level.startingLives)
            assertEquals("Level $id hints", 2, level.startingHints)
        }
        // Preference persistence itself is unchanged; these checks protect its
        // ID contract, not an unexecuted on-device migration claim.
    }

    @Test
    fun `master expansion preserves all one hundred approved puzzle layouts`() {
        // Frozen from baseline-100/catalog.json exported from the tested 100-level build.
        // Includes the current three-arrow Level 1, all IDs/cells/exits and presentation.
        val expected = listOf(
            "94ca02de400000acc56cc2c073bb059a15c0586596edf4d13e7bb837c4e38dbb",
            "22d13d32cef5ac85fe67fd1276908a7926f809a4854a0e24ff8e234445e4cd59",
            "55888af33b929b9e5ed67c6c2c618826f304e1804076e55707ac3a29951af359",
            "d19b4f3f4f33f2ffa015ae157cd24ab285d6c9173dc3084d42ada1a819100270",
            "d65db47f7fa53b5f7573bea4af2a9808bc2f3a5991702599c1809b6f9ac6c338",
            "fa15c2f9dd400701b1ffe8deeea512342624e8e1aabc1628073926b24b94c219",
            "7e8d016002af4414aef47eb86da7fb458b32ad653c996539aa5e9035109d738e",
            "08e8234914664aab02a5e90732ee4b00da58294141c27e33de8966ed7005ef96",
            "33b920a83369ffa9f0615b9c48a748d78820650d3724e4593ef3426bed9096cf",
            "ea700b6837fd4218eb4750caf1a84cc9f2bc9d67ea00ea565a48641e93adc794",
            "c5b8d093964cf92992a4f1b4294ada0f83d2fbfed4c1317a978d1b9a2882ec3f",
            "69af304533f3c3af67537b017a8a6b81a1a443a2c32d5428066eae3c71e35716",
            "f077fb1dbdd9486b5e5cf5da188047a482bae4cbe6694f7b292db0b1c977cad2",
            "7d4634ca8ee8673af41fb4299e2275d2e6104a3084d6125065cbfa07168a37b8",
            "ba41643fd70dcaeba5416dc7e165931fb1954f756e85f0716573f7ff7c896891",
            "3b21be69b68f0c47434d567f57a66cbde23cf3dadec80cba1283cc81ab6e2eb0",
            "9c937a281c888ab69dcd36d2f25ce32825640223376d1dac72211e7fe508c945",
            "1971da338d914ecd376285063362bebfeb9427a29e6db65c7c652d85080d8db9",
            "0305d2ce805ff47682104095fbd53e4aed0ad77e3fd8ca8bf7e34fbd391f3c8a",
            "d35639f0fe8e2a1d0d22eeccd492b63329080f7bf1dd6a4232caf9c9868f1d2a",
            "daf7fbd37aa9c40778656be4ee0a063308e04e4acb1ec937d471d9ab0b334538",
            "8d43fcbec75d3ce4d3908c6a187b5c26a6e326e4e465e5a031b9ebe3e2c18fae",
            "ce89bc0b69ae0bf937d8585ecca3a7302ec4a0a2fcc26d12d6ca370e411abc0e",
            "b8a4478af4f0a6a01db5bd596a64e98f624654f356bd546c1cd3ef612be3c4ec",
            "86e0e7b7bddf13a6244a211a9805a9735a26e525d496f60a16c91ccc18f83755",
            "14fd1bbf698948bf74f8fd6404c5acbe6954fc239066abe6deb0f6934e263291",
            "ffc002431b28c81948ab55020f9a9bed20d42a5b2b1e349ef637e6c52c416550",
            "fc25561c3c2856b07e353bee37801dfe2099de988758c5379203bc87c44fed78",
            "551e41caf2dda045f737677f20fe1159e0945097534c68b5239161d58cd3f408",
            "12ea216972969c1a3458dd006e32119b845bb2f4fdc6c6dfa7cef3a3fb69dfe0",
            "dedc67734081b7b5268cdc0c32d22ca3d6cd1687ee3e5137ef74db84592aeeb7",
            "5c0853869131aad4d3b579fe0252ad83c5bdec105a0802f4d8b067eddc780a18",
            "2ccf771b6dfcb2b33b4ea3efbc878683c6b741d68082a5a682b4691c1b9a57bc",
            "a09fa4f4141c640f35fb1a1d121752b67f815d2a40187fde182d705ffaeba3e0",
            "2882216961736e72b019e772361a54819c6853a71a057f9d45335c5021e4ffed",
            "6a97e6f2dc919cb8df9db8b203721bd6c7eacf90ad0b984a85e3086e1c913277",
            "fb8a193e69d57dcfa9dbd42c2204832d327259bb88fde425ac6df775a3a693dc",
            "0fb5008dbc6e43e748466255fedd74f14eeb23b6ece1c5adb3d7862f7aca2d95",
            "d384f4e3f0cdc4d74a6f8fa5d58eeebe910d31665cf45cc8b6f4baa44b476d66",
            "ff2b7c9a1e48574f7962e138e72a6ba75a5ef1ededd9765c515acc210272456e",
            "d519b0c3e449fa7492b3633f61b146845ec39d7850e02ff5f0cf8a7d7dd5e716",
            "0126ba6b2b8b241eaf26d4efaf53a1f494d73ca11b3e00860aeab45cbcdfe306",
            "e09ee8e01f8a3984ba5d5d627a46d220dbc7145a87b6aedb499a6b4966703f05",
            "1df28af99fe8c3904ceebba47b9ba92e56636f74f9d2ac764394072d6bc37c96",
            "f31320b1905151ccfdbfe6074770b3e7c04ce9ae418f75f1d780fdde40752b6a",
            "5d3b99cd6ee36a7837b5105fef318dec8056c871c927ea501e419815d645fb3a",
            "802906d63da26a8b690c3ee5f6edf7e2e86ef1d17c7752e0fa44178527a260fe",
            "07740a70e970a642f5604e29e20d749563f1f4ca90f2ca069ed3d47fec4e5452",
            "709cb0bef2ddc1323405ac88ca4e782afde6b9d2478e84b374116671581701d3",
            "b6f11eb34930aa0a88b3d88d7644fb451e542d1d75edc642fc03fa1642899b12",
            "c14dbaad1243e151bf82f96018b2099988b30794325abe8c48154828fbf44d7c",
            "0dffa9ce9b46f12688538d562c77e06a7f3f56b2c835d067a15f8bde9ede264c",
            "c98de5c4582b95c3bd38d616d67c700666a4f3449a9a25da6f7424de3e7cff46",
            "39608d340fc0364ee5457ac280a1000bab3c5771b4c43a12368bf61b1848abbc",
            "912c1d0d158cd4dc8c3ea7d8ed61fa1cb806412f833973928519cd780e89b089",
            "fcbe654e54dc63c34b7e5e580f02b80f90b1f375016e840ceef337507c6bc143",
            "21fc817988060e4d70fa90c7150d21c4ac28c98c9810f0490d62486f3e71e034",
            "a2198350b9002f637387d1a57ed5cb38c6d88969165eeb96fa580f39e9a880a5",
            "69593f50d0188793961645a44eee6e370b840026d2cbe65f87bf6dd6856b7363",
            "89f62bb4d3ff919eeee0c1c69bf918598447937aafaf6296696365417fccd15b",
            "362348ea3633e6058c46118e57133cdbb0e90abdd296d290956730a6966d1cfa",
            "50f2d4ab14bd074f3d451481bbb87a2024351c098627c25965a7c9d6e1b762d3",
            "8a3c7b44836caef8de791a24a77bd8be64fd4e3d886b9dd1bc1e6e7f38f0a488",
            "14ca3680f155af1e1ef1c1dd88cb7a0a2222c8c9bd6d6c89cbf9be7a764a1ec2",
            "9169c35f4b408775a76740bf16b9fcda532687a642383e56f06401362de217ee",
            "b2df2295b892312702a47885a9a6fa935c73a776243398bcb0cdd21b20e45c8b",
            "fd3b401242077e1cedf40e395a04fe6852dadff5e84343d011747deb9d6c1fc6",
            "d33dc05d9bda45a171c00c497d78c91e5316f6cae6b20e60c91862b6fed8f416",
            "742c67a42cf61b3e0834394ed74dbd3279d2656e1a297da665bd19eed7fe85d8",
            "3f08be206037fe1527bd89db9b781c3ec970aa73d9857fa9e545861cb4f3b68e",
            "62c2b8d78ab0133bf4631b27f9e0b53f159014f86270908ce9d195bdb801b1a1",
            "186055e0882e22fb0369fbe4f8c9d2f997a65bd042c5780ff369405f88db7f4b",
            "94e8868185c84bb07e33889247f14ad1ceb0478181e23f492158b66041791790",
            "399c7aa72e7066bb3ab9ba56a12f5ea15df0ef2979a61017a3d9f386f862cefb",
            "7493cab8c62487a832b8392e8e924fc5e72164678a2df91bfedbe0519dfea787",
            "2b2acd3d059a0bd856102b17ba1244a59a706d8d6c3a1c97dd1683dc0e718835",
            "9b4acfcca45c843af860b5caeaba084c6e13da5b1dd88d226feb57c93af86d9d",
            "8b8944064cad4ec92db398008f18ccbfe503010cd7843f2fa1f2321595219e7a",
            "1eb69275f954922fd3725bfb44138f5730f8f1a4db0551bfaf33bc4061c9cd61",
            "ba9c450f668943f6722d0ee1e6da5d7196644207edd0eb950ff3ae31710b998a",
            "cae368da7a085de40f11b2aa8a53b02c803215c6f8e4c3804c0092c44a5c5e82",
            "5367f849a20a42de59b24cf0675f572b04d5ccde5be43636760dbce854ec98be",
            "4682c3c9c77e164a9976b0b3fc129c9963d61b77cf373fd98210fe3c6d95a755",
            "e94fe060c36f73394c6acf0f73a5817c2c4afbc0c40c8b78afdd15bf59558478",
            "5d8150801150af6f763634381637c1c99458abcf80b9a45ae9a384ccce87189e",
            "ced97f948a5d785359f40cfba25d9042fbf0e8e2825c81888dd6e4762dfe7a01",
            "729fff02e976fb1c31bda1bc80c31f032871d8110f61d136eca64bd3919017d1",
            "ed86aabc656a51e1eea74d88609c437887904f02ee46d47238bcefe34544769f",
            "022308834564c6d123fe0edb3fe0f61779b9ca023f35fc373b771ea15b0f3593",
            "b750a4a99a46380cec5c858d0d3ab68f5771ab756bc7fce0aaf1647e6431813e",
            "779f0fd83d32bd9b0482710f0ad5a342a63d8ed6a7330328ba9458678a083be6",
            "4a982f21f4805480617d46d02d3762eb134ffc7614f66b57012d7b07914b8cce",
            "93cecd896eab286c44ba7253b25269087ce01e4aaccd804832ff4e3c9e44591a",
            "d77d175cbdb3d2ca1124e617c2d3cc0751ef57e5c4daf970f11cda279f8d6770",
            "387d55f0b5778f91f0476f2008ce898c69c4c0d8a00014b04d8fed5ddac5b600",
            "6339b3d4e916497b0df7503b4d8e32616a49077445fd84d97caaf92593f4e184",
            "69551a0ba6b458f2c211e1de3cf65e7965cfc47449ab05104bebc9bafb5b9ed4",
            "63e54eb822af967cef541f038d6c0211046903d67ac07bac9ad0cd2beb66c111",
            "3ac39fb806117a7076abb3a36cc91e6be6541ee6dbb486fad5e4a2d2eea298a0",
            "99c10944cf1c85c0f2f3997c5ad340989d87e3c6e54193383150c6a0e6842414"
        )
        for (id in 1..100) assertEquals("Approved level $id changed during Master expansion",
            expected[id - 1], geometryFingerprint(LevelCatalog.level(id)))
    }

    @Test
    fun `grandmaster expansion preserves all two hundred fifty approved puzzle layouts`() {
        // Frozen from the compiled, solver-replayed baseline-250 catalog before expansion.
        val expected = listOf(
            "94ca02de400000acc56cc2c073bb059a15c0586596edf4d13e7bb837c4e38dbb",
            "22d13d32cef5ac85fe67fd1276908a7926f809a4854a0e24ff8e234445e4cd59",
            "55888af33b929b9e5ed67c6c2c618826f304e1804076e55707ac3a29951af359",
            "d19b4f3f4f33f2ffa015ae157cd24ab285d6c9173dc3084d42ada1a819100270",
            "d65db47f7fa53b5f7573bea4af2a9808bc2f3a5991702599c1809b6f9ac6c338",
            "fa15c2f9dd400701b1ffe8deeea512342624e8e1aabc1628073926b24b94c219",
            "7e8d016002af4414aef47eb86da7fb458b32ad653c996539aa5e9035109d738e",
            "08e8234914664aab02a5e90732ee4b00da58294141c27e33de8966ed7005ef96",
            "33b920a83369ffa9f0615b9c48a748d78820650d3724e4593ef3426bed9096cf",
            "ea700b6837fd4218eb4750caf1a84cc9f2bc9d67ea00ea565a48641e93adc794",
            "c5b8d093964cf92992a4f1b4294ada0f83d2fbfed4c1317a978d1b9a2882ec3f",
            "69af304533f3c3af67537b017a8a6b81a1a443a2c32d5428066eae3c71e35716",
            "f077fb1dbdd9486b5e5cf5da188047a482bae4cbe6694f7b292db0b1c977cad2",
            "7d4634ca8ee8673af41fb4299e2275d2e6104a3084d6125065cbfa07168a37b8",
            "ba41643fd70dcaeba5416dc7e165931fb1954f756e85f0716573f7ff7c896891",
            "3b21be69b68f0c47434d567f57a66cbde23cf3dadec80cba1283cc81ab6e2eb0",
            "9c937a281c888ab69dcd36d2f25ce32825640223376d1dac72211e7fe508c945",
            "1971da338d914ecd376285063362bebfeb9427a29e6db65c7c652d85080d8db9",
            "0305d2ce805ff47682104095fbd53e4aed0ad77e3fd8ca8bf7e34fbd391f3c8a",
            "d35639f0fe8e2a1d0d22eeccd492b63329080f7bf1dd6a4232caf9c9868f1d2a",
            "daf7fbd37aa9c40778656be4ee0a063308e04e4acb1ec937d471d9ab0b334538",
            "8d43fcbec75d3ce4d3908c6a187b5c26a6e326e4e465e5a031b9ebe3e2c18fae",
            "ce89bc0b69ae0bf937d8585ecca3a7302ec4a0a2fcc26d12d6ca370e411abc0e",
            "b8a4478af4f0a6a01db5bd596a64e98f624654f356bd546c1cd3ef612be3c4ec",
            "86e0e7b7bddf13a6244a211a9805a9735a26e525d496f60a16c91ccc18f83755",
            "14fd1bbf698948bf74f8fd6404c5acbe6954fc239066abe6deb0f6934e263291",
            "ffc002431b28c81948ab55020f9a9bed20d42a5b2b1e349ef637e6c52c416550",
            "fc25561c3c2856b07e353bee37801dfe2099de988758c5379203bc87c44fed78",
            "551e41caf2dda045f737677f20fe1159e0945097534c68b5239161d58cd3f408",
            "12ea216972969c1a3458dd006e32119b845bb2f4fdc6c6dfa7cef3a3fb69dfe0",
            "dedc67734081b7b5268cdc0c32d22ca3d6cd1687ee3e5137ef74db84592aeeb7",
            "5c0853869131aad4d3b579fe0252ad83c5bdec105a0802f4d8b067eddc780a18",
            "2ccf771b6dfcb2b33b4ea3efbc878683c6b741d68082a5a682b4691c1b9a57bc",
            "a09fa4f4141c640f35fb1a1d121752b67f815d2a40187fde182d705ffaeba3e0",
            "2882216961736e72b019e772361a54819c6853a71a057f9d45335c5021e4ffed",
            "6a97e6f2dc919cb8df9db8b203721bd6c7eacf90ad0b984a85e3086e1c913277",
            "fb8a193e69d57dcfa9dbd42c2204832d327259bb88fde425ac6df775a3a693dc",
            "0fb5008dbc6e43e748466255fedd74f14eeb23b6ece1c5adb3d7862f7aca2d95",
            "d384f4e3f0cdc4d74a6f8fa5d58eeebe910d31665cf45cc8b6f4baa44b476d66",
            "ff2b7c9a1e48574f7962e138e72a6ba75a5ef1ededd9765c515acc210272456e",
            "d519b0c3e449fa7492b3633f61b146845ec39d7850e02ff5f0cf8a7d7dd5e716",
            "0126ba6b2b8b241eaf26d4efaf53a1f494d73ca11b3e00860aeab45cbcdfe306",
            "e09ee8e01f8a3984ba5d5d627a46d220dbc7145a87b6aedb499a6b4966703f05",
            "1df28af99fe8c3904ceebba47b9ba92e56636f74f9d2ac764394072d6bc37c96",
            "f31320b1905151ccfdbfe6074770b3e7c04ce9ae418f75f1d780fdde40752b6a",
            "5d3b99cd6ee36a7837b5105fef318dec8056c871c927ea501e419815d645fb3a",
            "802906d63da26a8b690c3ee5f6edf7e2e86ef1d17c7752e0fa44178527a260fe",
            "07740a70e970a642f5604e29e20d749563f1f4ca90f2ca069ed3d47fec4e5452",
            "709cb0bef2ddc1323405ac88ca4e782afde6b9d2478e84b374116671581701d3",
            "b6f11eb34930aa0a88b3d88d7644fb451e542d1d75edc642fc03fa1642899b12",
            "c14dbaad1243e151bf82f96018b2099988b30794325abe8c48154828fbf44d7c",
            "0dffa9ce9b46f12688538d562c77e06a7f3f56b2c835d067a15f8bde9ede264c",
            "c98de5c4582b95c3bd38d616d67c700666a4f3449a9a25da6f7424de3e7cff46",
            "39608d340fc0364ee5457ac280a1000bab3c5771b4c43a12368bf61b1848abbc",
            "912c1d0d158cd4dc8c3ea7d8ed61fa1cb806412f833973928519cd780e89b089",
            "fcbe654e54dc63c34b7e5e580f02b80f90b1f375016e840ceef337507c6bc143",
            "21fc817988060e4d70fa90c7150d21c4ac28c98c9810f0490d62486f3e71e034",
            "a2198350b9002f637387d1a57ed5cb38c6d88969165eeb96fa580f39e9a880a5",
            "69593f50d0188793961645a44eee6e370b840026d2cbe65f87bf6dd6856b7363",
            "89f62bb4d3ff919eeee0c1c69bf918598447937aafaf6296696365417fccd15b",
            "362348ea3633e6058c46118e57133cdbb0e90abdd296d290956730a6966d1cfa",
            "50f2d4ab14bd074f3d451481bbb87a2024351c098627c25965a7c9d6e1b762d3",
            "8a3c7b44836caef8de791a24a77bd8be64fd4e3d886b9dd1bc1e6e7f38f0a488",
            "14ca3680f155af1e1ef1c1dd88cb7a0a2222c8c9bd6d6c89cbf9be7a764a1ec2",
            "9169c35f4b408775a76740bf16b9fcda532687a642383e56f06401362de217ee",
            "b2df2295b892312702a47885a9a6fa935c73a776243398bcb0cdd21b20e45c8b",
            "fd3b401242077e1cedf40e395a04fe6852dadff5e84343d011747deb9d6c1fc6",
            "d33dc05d9bda45a171c00c497d78c91e5316f6cae6b20e60c91862b6fed8f416",
            "742c67a42cf61b3e0834394ed74dbd3279d2656e1a297da665bd19eed7fe85d8",
            "3f08be206037fe1527bd89db9b781c3ec970aa73d9857fa9e545861cb4f3b68e",
            "62c2b8d78ab0133bf4631b27f9e0b53f159014f86270908ce9d195bdb801b1a1",
            "186055e0882e22fb0369fbe4f8c9d2f997a65bd042c5780ff369405f88db7f4b",
            "94e8868185c84bb07e33889247f14ad1ceb0478181e23f492158b66041791790",
            "399c7aa72e7066bb3ab9ba56a12f5ea15df0ef2979a61017a3d9f386f862cefb",
            "7493cab8c62487a832b8392e8e924fc5e72164678a2df91bfedbe0519dfea787",
            "2b2acd3d059a0bd856102b17ba1244a59a706d8d6c3a1c97dd1683dc0e718835",
            "9b4acfcca45c843af860b5caeaba084c6e13da5b1dd88d226feb57c93af86d9d",
            "8b8944064cad4ec92db398008f18ccbfe503010cd7843f2fa1f2321595219e7a",
            "1eb69275f954922fd3725bfb44138f5730f8f1a4db0551bfaf33bc4061c9cd61",
            "ba9c450f668943f6722d0ee1e6da5d7196644207edd0eb950ff3ae31710b998a",
            "cae368da7a085de40f11b2aa8a53b02c803215c6f8e4c3804c0092c44a5c5e82",
            "5367f849a20a42de59b24cf0675f572b04d5ccde5be43636760dbce854ec98be",
            "4682c3c9c77e164a9976b0b3fc129c9963d61b77cf373fd98210fe3c6d95a755",
            "e94fe060c36f73394c6acf0f73a5817c2c4afbc0c40c8b78afdd15bf59558478",
            "5d8150801150af6f763634381637c1c99458abcf80b9a45ae9a384ccce87189e",
            "ced97f948a5d785359f40cfba25d9042fbf0e8e2825c81888dd6e4762dfe7a01",
            "729fff02e976fb1c31bda1bc80c31f032871d8110f61d136eca64bd3919017d1",
            "ed86aabc656a51e1eea74d88609c437887904f02ee46d47238bcefe34544769f",
            "022308834564c6d123fe0edb3fe0f61779b9ca023f35fc373b771ea15b0f3593",
            "b750a4a99a46380cec5c858d0d3ab68f5771ab756bc7fce0aaf1647e6431813e",
            "779f0fd83d32bd9b0482710f0ad5a342a63d8ed6a7330328ba9458678a083be6",
            "4a982f21f4805480617d46d02d3762eb134ffc7614f66b57012d7b07914b8cce",
            "93cecd896eab286c44ba7253b25269087ce01e4aaccd804832ff4e3c9e44591a",
            "d77d175cbdb3d2ca1124e617c2d3cc0751ef57e5c4daf970f11cda279f8d6770",
            "387d55f0b5778f91f0476f2008ce898c69c4c0d8a00014b04d8fed5ddac5b600",
            "6339b3d4e916497b0df7503b4d8e32616a49077445fd84d97caaf92593f4e184",
            "69551a0ba6b458f2c211e1de3cf65e7965cfc47449ab05104bebc9bafb5b9ed4",
            "63e54eb822af967cef541f038d6c0211046903d67ac07bac9ad0cd2beb66c111",
            "3ac39fb806117a7076abb3a36cc91e6be6541ee6dbb486fad5e4a2d2eea298a0",
            "99c10944cf1c85c0f2f3997c5ad340989d87e3c6e54193383150c6a0e6842414",
            "bfd50dddd71c3bfc948860d6954deea3dedf2b761b7129b42b0968173ad06298",
            "165eeacf0f2cb5bca934078eb283d432e46981be90d3bb0f642736b0ea2fc858",
            "4c13822a913ae950049aa2faf30ec8ac3b5949a64b9b4c18c999cc1857fb5aa7",
            "61fab1f933e14fb1437a794d70e7bafa4503a0d2c0eed3d1d62085e887d498c5",
            "c0dd41713914aafba4110e84207e9a97143e4012ddfa506b525f2db819d96a2e",
            "e7f47ef7670238ce53872d2107bc7155209f3f851d803a929d1da13a9b985382",
            "0f54c1919f8d1c201a0f42a71565d254ce746e9edda1ef635ae417cf2b4d7331",
            "8ef29fcf530c746f318fd814f6629b9cc2594cefe0d320ffe1d4d7c049709391",
            "de5a105acc7efe34bdb9be45b897885bf3a70e2d2aa31d1ee2803a4a337e8705",
            "93bf8d94df73e644337c748d2e1b7fbcaf82efd6f54bdd06dfb6d0e4c28e08b9",
            "ee4a9351ff3b39a07f14d5cb64b0d5b0db4ac34b65628a86be436c6939adced1",
            "e230e85211cd0772906732cf32a4c0909f5a9fbb50b14ecd55f9ff6b370c04fd",
            "9362cb402ac61e3df16455ee8a856942e3ba75730aa0366fc8bc24d27ef07f6d",
            "d18bf9b21685fd05dbd09c315e03670957986effdea9c81c6b6484971467af2e",
            "67a3998d82c0dd4cf7aa9b6bda69ac4f08750fe1194b424c2e11a8da28e7bb04",
            "5d63cef14ac792746ddc7037eef988fa55acc5091bd8c86b5ed8d65db299d681",
            "0252fc591bfd105f4f4efd7bfe614aab407f01d99e251661cd1cf8ce6d8b6212",
            "eea467ce19bf4b87208849151ea2e910e07320303534b65a984b760484cb12b5",
            "ce72236cab4ef8e57f77bc19cb952bb00b4fe5cffa75c28bd763a5355b39527e",
            "ac57e291a362df4c0b5a9fe5d1f7df8973bdeb1a561eb66ba0fb9c4bf185706c",
            "d92d1f52cb359a375a37c6ea0256315a7f3b8fdb44132dfb90d5087d181d264e",
            "f59e9c850347630b0d4d53449fe5a159cf54f7f0dd583b1fc8f21782a0be4bdf",
            "36fb937f19efaaf19a4dcc70e90c0723335c52e7172735d3668c6e108afebe62",
            "37b5b5e21d45760e737ce3af61cde1b9b487a1456b1455e2d6a17940bafd643a",
            "769ad1b179daf177ff2cf980cca326d8a23381ee72072c6a8a3fa413b78db8bd",
            "5dd510ac8a5d322999b5822e247a0d526387e2f791bb261daed9ce18840de1bf",
            "06ba7aad6cf9dfe17f48d6c76b3fa1f022d19712af5bf7903b055d88241d661c",
            "8176c3c2a05bafeed55631c7eaf1076a88964e23287bd42eb20f66d0e8c3045c",
            "71d43daf24690bdb282b73c7e5a3f9e7a7c49fa4de667f238f1765117bee298e",
            "e9d8aa1e1f69e95a8e4114411487ea2d7421c864b7fe34550369a5545b7d288c",
            "b3d67ad3b5823c8bba77d8fc98d2b129bb17cb4148851815db74dc95e7f832aa",
            "f992303100f6c44d82cc1fb0243ac1a3fbad59a02eea6e8c29930a95d72a71dc",
            "e3e79611b2ffe48b9195e72bde6733b0a456f97271a9992dac060035309fb31a",
            "975f9ebc215a0196ed54003de581a62997894969804ae9619cebb0316022ce1d",
            "0d65c26432f985a7a3b8216f00ab505535cf09134df85a5b0ad2d7f0f742dea6",
            "88b7b2dde1487a82ab7d361dfb14a9c879d753545eb7f39f3c98e2430bcdb0d0",
            "f2085aaf3b28ac3d8d29b7a7ae8c5c0e0f5a6d7949ac626c587a5bf9fca4aba9",
            "58cf328aece17865d2351d61af270b635440d8e7886ebfe451e48ca74f693c83",
            "040047b58f683075c77bcdeef34b6b11b6fd5f2ac37e7959fa3d3ea0285e8950",
            "e880d73591d129293c84f69a3b24bf98351459d85e31c2abbc798e39074f2449",
            "5aa2859af0cd58b20827fc199013a1dadd1022799e272fb1c106ca8c72e60ec5",
            "ef091b5a40d9b47d32b3dfe7458623d830611993de8e25bde40605827f234978",
            "29f21dfa66152cdb9b541ca36987620e5232d1526bbac1f2538170ec3cccf418",
            "fbd7e9406922e5292d92b2fd8b971a6c757e6668ed50d02620b6ca96b7884514",
            "cca0373a135f086d8b2cae3d201ced4e080eebedd9c7a0ace6b7e8b0af002f08",
            "7d924bf2667db5968653ff8e4282b9ee44995010391679d80df8602e22852ce5",
            "5dbf3a836d2299e17aee1062ec764bfc5e8c483e02df985fb19e80a467a60958",
            "b9b223c340c6ace139065ff91272008c0fb62bc5771b408c91f5b950375c4956",
            "40b356b876f6529e612e3c7aea2660347e3d0d9100d8423fa87570d904e86177",
            "920f09f79dd5ae19fc2cca251203cc520cc389dc705fd868e59e5df0a4b76a5c",
            "722c927aafdb3c2b2a43f1705f8a544dc9f81008bea154b8312e1177cfb0bb22",
            "8b664543df97dd2319b9ea79d05879f60c1fc40eba54803f96b8ff7473bde837",
            "73132daecb8fd33cff61163bc5f2a26ddeba9a66710ec1a98acca09474397e8c",
            "12eede7bfb1dab511c94f65d916f19685cc6533480ad46b435eedee0116a5b85",
            "fc7e1f075b8711e5b10e3fd26a77e5d11d1bbb13202bd8500e2dd6c3594e5f20",
            "bdd42e4ccfd0584e0c384402edfbf34119b027effcea6a1f687182c83989ab2f",
            "e186ed4a7d775155d0854177c5b7da92a87fc02635b1a9de83cd51fde5eb7a5a",
            "e56d97ad87cae7f265f4fb2434bbf7f6c8a126772c29467ac34c7a9cf1217f46",
            "1e71df1650e4e103750494b64c489b79740ede32a65d74f63aff73ccef08cb6c",
            "5d576bab48b1282982d0aa4c993b165545ebf53f9515fa2c40056ea87b92fbb3",
            "52ba5f0a17beca4d73cab9e32c206e0e47443d470710f99275f65ea3e05317dd",
            "0162a69a483dd90902ba8e10caf7d313b0c314e4cca08c836336fb4bb2e4c9d0",
            "fb748f6705c680d78565dd4739a8815bf1cb8b10ba01ac6f4f28ec219b269f72",
            "a1b1add85e0a2aef6ba66925619bfde7506e437812a506cbdeb3d73fadb6b504",
            "3e7c8ebd1a7be7894fd72696b7045301e3e2728864685f4dae9a078e540c5893",
            "fe62dccde6d3b957c2b4917b4c7e71ed8109ff61a6be2866a2eff30270653fa5",
            "7b34c483debfe8997db4b89c9cfd2ccb803cfa1a7b90f0a751688e896d8f3c4e",
            "72c1ab965b759baf6497e07a61a395fe212e586001001a305c0dae1cfd8f40e8",
            "2e520161c73ea4d9e4d9f5014a6c45a30f7f7c1ca4942bdf02033842fe2eb3df",
            "819a791a99e0017c27a329b85bd17bec2d1d968011b62170ab8f38a9156d6479",
            "6d8d260fedcc22eff463b48f28c60a662a18e9542ebcc9389460ca171393ec4f",
            "9afa511fb0af86886ae9a48c3d9a23719c598a8fb6e50d4b90f92c4e9cc4a5ef",
            "786f67f525a4bdf78755a56bbd0fa15c652704c123c09241aa658efbc7f35919",
            "12e8892eaae3b6dd3792b56a3e86d95322b408d7b7b9188d3f19543ab562f9d6",
            "1455e9058c07f5876e9f757cedbc3a9ef0e28eacd0779dcd1c02f90ddc1ec72e",
            "87ff8790957b536fe606cd464860bef4b264f156310cbd6c5862bd316e726b4e",
            "4309865fe809e349e5c48924462c5107aa4c4ec31e28f35d7aacb35d40ffdac0",
            "1f82f3ec3edf037f0e5aa372dab3c5507dee93a4e1051a7d7420b0a2593fedee",
            "9b9b3130250c288cda5ef029c77a2f91fb6106156a62815262b0d25124731527",
            "757ec900f420c55afe94de0c9701b6f45b6b61dd4b0de6c6deb2f54462e40361",
            "25f64b724858c9fc353b27fc8010a6e5e185219d360a0ba44f2ce97df29aa305",
            "72d3ea00a436238b4ec4362c33d8ca9f9df4171d66868e278e13b7d51a844b1e",
            "bfb4f977b8bf0990a08b8f4a3ba4aeef0ce361e0d6be3f9f59e24e839339737b",
            "1367710214dadb8e77cc721bb72da4895e3b1ec5a10f1a9ed466d7e25a4b6709",
            "c269cdcba2f583ad6d905393acf790084b56e66ad4074e8be81214c5cb94d01c",
            "246f7f9dbf376fc00d9d26dc3815ec2e45af1f92851376c0a930fc6d64da424d",
            "26a4a357ad2a0ec6ea8df0473c56688094a62591981f58bc3edd3cc4cd38bacd",
            "2e7021238d418d148e521dbd06df104a4a5d232d7af40833f4d7064aa8931ddf",
            "20038f58d413b933f680945592da6fe9229e4379590668ddbfc93a3c8d19e945",
            "47e65b35d8631bc0d6da2e2e15e568b89cf8621d5c2f43edccfe8b28d4d623fc",
            "1038b1ca6007195eb0de997913a357c7a595c8676abdbc38118090ced3e05027",
            "9b5081e0356e76d96c92f64c0286f7a2aed8b62533a746f7c1d05569770cb388",
            "99f6b51d2af6401159579cb35ed708c986418bb382580760d18e70088e74d2ff",
            "8489be51d9837088c8456000f946bf77799c55911b22fd446259e1e02decb52b",
            "74a30956457918eb8adb7334b505ca5ce0f438593991b1a23ba753e7be3099cd",
            "960642520520aa9671b8b991f12eb7e97fcfe650a022312da2bd5606f48655e7",
            "3a05721cfc0950376188318951643a72569d9ef9693238b061d2ca46cc485695",
            "bbd108bbced904b5793602207c33fffb0a71604989486877707843f9229e0dc3",
            "b27e018361bf9850adae44b12370d9a992de1c5e99ebac71b41471c7cf012107",
            "fb9de74717051e4937b78c546a70c23b5c28b9900d23716d4f7ecec02dfdc326",
            "eab63082a1da133256a2edbb85c2460d5103dc18693a8cedaf87afa07d10bb38",
            "027d35eb1bb3c8382fa9ac050601e99bde50681dd6fb0b054a719be264b1b794",
            "da47d0303441e91fd4390c88b8473d97907fe4e9a1adf0f5d9506772c079bad8",
            "2115d216752a3d9a728d96b7a2f27b576e3287a1e3b4f5dcdc363cf56af1f30f",
            "bfe5e27fea977e0244d4f3e4d7cb79aac096050624c219cfdd5ea7fac14680cc",
            "4c093c3b871fc7c168fbf88f6c5c0ae2cdbacbb2e40ca3667e4059c61ff8fb79",
            "5e1c67aae3e2549981fbe72759001f927d48ee97990037dae468767e6a154dd3",
            "a30ea2d41d77bca67f376ca96772bdec846f3cf67ef8094f746e060f17fdce66",
            "9e23de1e223579f7fcd2d25511470502c8f7c845fefadef5750214e31e060bb4",
            "3fd43ceff20a309ba206eacfb5e2ec79ec68b6a393317e905f080bad4b01f47f",
            "0b46e6546372ebdae7d2fe577dc3da174ee3c4b471d30fd3a43081d294a96d9b",
            "d61a6a64a4dca8e35914ae479fae3ad9a069636aa4f5213270841d8d150c7180",
            "cbdbc05b6c707f4a98eb5992280cdb86e0cf5cbe57c81dba4b38773113577c58",
            "632488e1a48051fb03a9321cee5bb439bc5b0963d5ae7ae15a05e6d2f851bb67",
            "a5eb92aed80f7e840411095067db17c5e6b381a296b9c5becf0667a6a2adadb6",
            "d8b48e86912decef65f6e5f2b2d57c23723d4962caba6feea1ed8d63848dab46",
            "f367fb6f6613303c160354e8ef92219340cece7e796f9b122167754f187af13e",
            "3b629b8a8b2b2aa3790797fd0af42f9bb946167aa275e4ba576c167ce58b868c",
            "021d05ea71f34b6158a131a84c6a3faeb50fd6cfe12c1de717d3f101d5bd6de6",
            "e9ff92ab598b4e0536c0059f568ba35050eb922c73aa801e343b28838a76cda1",
            "304c06c079e3e353b4da4516eebd052da91552d209e273e441c38983099245b7",
            "73e9a48ea9054dabb26411c731dbfa1ef48776329dd46559a281b750dd30bc63",
            "d564a9e3a4cfe2a4e3a1c1ef5a072bd56a858b9a6af8532d2a086e2e8527959a",
            "d4bea534c1cb43086a4d215fd8cf2afd96d09d712e93831a1bfba46ad96f7b41",
            "b7368fc7d8bfca4de26aa94ca07b964a8aa4d26b142204cadbb7e6ac479ab328",
            "ec778e22e0a03990249f46c7df00a8488245781566f3c0ea8f037b8f0ab9e525",
            "3e1146b04216f440884cd69a513a09a7877c84bb4a8ecbf348dc0179ea437033",
            "032fce86675fe50974fadc96eaf82cb4c632badb57afa707d97ec70c7e972c79",
            "e7eac08b7d9843a44db4f264b259a1dd7720456076db6beea494eb57574fb213",
            "16af7197a012571005d891b3011a4ea1408de7fc9102407063075a4d1d49c779",
            "7e789b6c2c918e5c006cdacf013cf766a30cd04ad7881e69dc4f866bd15d5d8d",
            "96d7d315e2b28f0ac2653221fefc265ed63be31eeffbf0c5d5e7564aa900456a",
            "8c31d8970c9958bbe125a96de4af0a37276e4e3279c25f892509a77cfaf4a58c",
            "350f5da9f2d54c396bd6732f1477d69b2ba3d8a4eba7751bb96d4dd8a48ef675",
            "793a392e5998d164dd928ec5ee45a6e4eb7d19824b8e7719fbfe49448f340cf7",
            "36a5aedc7702606c40261e361308c99f72ed2094f0bc5106322b87e19ed2349f",
            "b6dfbc89a787eaf59a936b76f06ae8eb473007cf38ede3dcffda169a593be099",
            "8feb3a5939a8e1c884fbe28ded3690c21690c9596420b2ac511998b89a862d7e",
            "8f80cdd52c5a965d75b2d9618ee64b1b905cf8303a1da80e6b7767d91c2fa870",
            "5a7010716dec8135b7b99fe44c1763d8699954cea8781b628ee32da61053fc5f",
            "061e7a48643a7b323d6098d2e8a04d931f61a84e6bbe878a0ff8d7555ca19b11",
            "4f38412b2fd3df871f55b74f3e82ea0eb028cd862899a50f6f913bbee7f48261",
            "0a65d9daa76c01067eb448c37a10e935633e164f2e10cc37bb5eef934ffd8cb5",
            "b68d7e274a1c6148fd86ad268d1b3648deac693efc7c88bb61a19bc7bbfc80b4",
            "feb20c517f74fa5065b7d21116ca4d2edbc2193ee3b958d26c40a2fcfbf3cc75",
            "f8f14ac5292a596c0a420b8e0393f1d6a4cca50af89ea6bf571e28b8a1e8ee53",
            "e37c170af99e2f55c9e688e5d836a65c1650e7ff95e5110d3f836aedbd35ee88",
            "197341490e1faedef5de74fce86b69340364946f8929230cc11e59b434bd34fb",
            "b6ac9a64076a1277f4845ed863e29d1f672953999b5037853cc3ff19abe2a8c1",
            "da7550c9516d37c59ae9c35261b640cf7606256c4606556fea31865bb2cf62e5"
        )
        for (id in 1..250) assertEquals("Approved level $id changed during Grandmaster expansion",
            expected[id - 1], geometryFingerprint(LevelCatalog.level(id)))
    }

    @Test
    fun `legend and extreme expansion preserves all five hundred approved puzzle layouts`() {
        // Frozen from artifacts/campaign/baseline-500/catalog.json before expansion.
        // Includes dimensions, ordered arrow IDs, every cell, exits, style and movement rule.
        val expected = requireNotNull(javaClass.getResourceAsStream("/campaign-500-geometry.sha256"))
            .bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().filter { it.isNotBlank() }.map { row ->
                    val fields = row.split('\t')
                    fields[0].toInt() to fields[1]
                }.toList()
            }
        assertEquals((1..500).toList(), expected.map { it.first })
        for ((id, fingerprint) in expected) {
            assertEquals("Approved level $id changed during Legend and Extreme expansion",
                fingerprint, geometryFingerprint(LevelCatalog.level(id)))
        }
    }

    @Test
    fun `apex expansion preserves all one thousand approved puzzle layouts`() {
        // Captured only after the compiled 1000-level campaign passed its full suite.
        val expected = requireNotNull(javaClass.getResourceAsStream("/campaign-1000-geometry.sha256"))
            .bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().filter { it.isNotBlank() }.map { row ->
                    val fields = row.split('\t')
                    fields[0].toInt() to fields[1]
                }.toList()
            }
        assertEquals((1..1000).toList(), expected.map { it.first })
        for ((id, fingerprint) in expected) {
            assertEquals("Approved level $id changed during Apex expansion",
                fingerprint, geometryFingerprint(LevelCatalog.level(id)))
        }
    }

    private fun geometryFingerprint(level: Level): String {
        val canonical = buildString {
            append(level.cols).append('|').append(level.rows).append('|')
                .append(level.presentation.name).append('|').append(level.movementRule.name).append('\n')
            for (arrow in level.arrows) {
                append(arrow.id).append(':').append(arrow.exit.name).append(':')
                for (cell in arrow.cells) append(cell.col).append(',').append(cell.row).append(';')
                append('\n')
            }
        }
        return sha256(canonical)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
