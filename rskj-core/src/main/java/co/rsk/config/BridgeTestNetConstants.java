/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.config;

import co.rsk.bitcoinj.core.BtcECKey;
import co.rsk.bitcoinj.core.Coin;
import co.rsk.bitcoinj.core.NetworkParameters;
import co.rsk.peg.AddressBasedAuthorizer;
import co.rsk.peg.Federation;
import co.rsk.peg.FederationMember;
import java.util.ArrayList;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.crypto.ECKey;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BridgeTestNetConstants extends BridgeConstants {
    private static BridgeTestNetConstants instance = new BridgeTestNetConstants();

    BridgeTestNetConstants() {
        btcParamsString = NetworkParameters.ID_TESTNET;

        BtcECKey federator0PublicKey = BtcECKey.fromPublicOnly(Hex.decode("02a7ff948f7bf0f1d2f6f425922490e20adf32bd6c5f75f4b4364cacdc7f4cbf8e"));
        BtcECKey federator1PublicKey = BtcECKey.fromPublicOnly(Hex.decode("034cc7cbe4c1dfcd0f32385a8a2b1de4b79098cd3429b8944b16157af78b2b38ec"));
        BtcECKey federator2PublicKey = BtcECKey.fromPublicOnly(Hex.decode("02b207b40ec4a7d86bf4abaa2461b57ee408d689d0ce21851c5fd6ece05ef35c04"));

        List<BtcECKey> genesisFederationPublicKeys = Arrays.asList(federator0PublicKey, federator1PublicKey, federator2PublicKey);

        // IMPORTANT: BTC, RSK and MST keys are the same.
        // Change upon implementation of the <INSERT FORK NAME HERE> fork.
        List<FederationMember> federationMembers = FederationMember.getFederationMembersFromKeys(genesisFederationPublicKeys);

        // Currently set to: 2021-11-01T03:00:00.000Z
        Instant genesisFederationAddressCreatedAt = Instant.ofEpochMilli(1635735600l);

        genesisFederation = new Federation(
                federationMembers,
                genesisFederationAddressCreatedAt,
                1L,
                getBtcParams()
        );

        btc2RskMinimumAcceptableConfirmations = 2;
        btc2RskMinimumAcceptableConfirmationsOnRsk = 2;
        rsk2BtcMinimumAcceptableConfirmations = 5;

        updateBridgeExecutionPeriod = 3 * 60 * 1000; // 3 minutes

        maxBtcHeadersPerRskBlock = 500;

        legacyMinimumPeginTxValueInSatoshis = Coin.valueOf(1_000_000);
        minimumPeginTxValueInSatoshis = Coin.valueOf(500_000);
        legacyMinimumPegoutTxValueInSatoshis = Coin.valueOf(500_000);
        minimumPegoutTxValueInSatoshis = Coin.valueOf(250_000);

        // Passphrases are kept private
        List<ECKey> federationChangeAuthorizedKeys = Arrays.stream(new String[]{
                "0497030259c183747b49ee521193697a413d3276b23b61b821b36341a84c92321284c5e53b04d186160910397cb29927434b0f207ee85eca47a5852ab7a72f7ba3",
                "0426b13d46d72749fe20b024d38dd43a81950c31e42e61ba5cf900d0a25c801c762c159165d4003d4d244bd8ebca6d881a585d11570e3108df1f395130df0a50ec",
                "04b5cbdf93caae51e14838f8791b53df575c7cd03ef75b8eb2fc6314e4b33ebd50709e689ebf5413cd45dcd7903a8fed1c2bc0e0323e0180691242d3f20f85ae37"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        federationChangeAuthorizer = new AddressBasedAuthorizer(
                federationChangeAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        // Passphrases are kept private
        List<ECKey> lockWhitelistAuthorizedKeys = Arrays.stream(new String[]{
                "0497030259c183747b49ee521193697a413d3276b23b61b821b36341a84c92321284c5e53b04d186160910397cb29927434b0f207ee85eca47a5852ab7a72f7ba3"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        lockWhitelistChangeAuthorizer = new AddressBasedAuthorizer(
                lockWhitelistAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.ONE
        );

        federationActivationAge = 60L;

        fundsMigrationAgeSinceActivationBegin = 60L;
        fundsMigrationAgeSinceActivationEnd = 900L;

        List<ECKey> feePerKbAuthorizedKeys = Arrays.stream(new String[]{
                "0497030259c183747b49ee521193697a413d3276b23b61b821b36341a84c92321284c5e53b04d186160910397cb29927434b0f207ee85eca47a5852ab7a72f7ba3",
                "0426b13d46d72749fe20b024d38dd43a81950c31e42e61ba5cf900d0a25c801c762c159165d4003d4d244bd8ebca6d881a585d11570e3108df1f395130df0a50ec",
                "04b5cbdf93caae51e14838f8791b53df575c7cd03ef75b8eb2fc6314e4b33ebd50709e689ebf5413cd45dcd7903a8fed1c2bc0e0323e0180691242d3f20f85ae37"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        feePerKbChangeAuthorizer = new AddressBasedAuthorizer(
                feePerKbAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.MAJORITY
        );

        genesisFeePerKb = Coin.MILLICOIN;

        maxFeePerKb = Coin.valueOf(5_000_000L);

        List<ECKey> increaseLockingCapAuthorizedKeys = Arrays.stream(new String[]{
                "0497030259c183747b49ee521193697a413d3276b23b61b821b36341a84c92321284c5e53b04d186160910397cb29927434b0f207ee85eca47a5852ab7a72f7ba3",
                "0426b13d46d72749fe20b024d38dd43a81950c31e42e61ba5cf900d0a25c801c762c159165d4003d4d244bd8ebca6d881a585d11570e3108df1f395130df0a50ec",
                "04b5cbdf93caae51e14838f8791b53df575c7cd03ef75b8eb2fc6314e4b33ebd50709e689ebf5413cd45dcd7903a8fed1c2bc0e0323e0180691242d3f20f85ae37"
        }).map(hex -> ECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        increaseLockingCapAuthorizer = new AddressBasedAuthorizer(
                increaseLockingCapAuthorizedKeys,
                AddressBasedAuthorizer.MinimumRequiredCalculation.ONE
        );

        lockingCapIncrementsMultiplier = 2;
        initialLockingCap = Coin.COIN.multiply(200); // 200 BTC

        btcHeightWhenBlockIndexActivates = 2_039_594;
        maxDepthToSearchBlocksBelowIndexActivation = 4_320; // 30 days in BTC blocks (considering 1 block every 10 minutes)

        erpFedActivationDelay = 52_560; // 1 year in BTC blocks (considering 1 block every 10 minutes)

        erpFedPubKeysList = Arrays.stream(new String[] {
            "0216c23b2ea8e4f11c3f9e22711addb1d16a93964796913830856b568cc3ea21d3",
            "034db69f2112f4fb1bb6141bf6e2bd6631f0484d0bd95b16767902c9fe219d4a6f",
            "0275562901dd8faae20de0a4166362a4f82188db77dbed4ca887422ea1ec185f14"
        }).map(hex -> BtcECKey.fromPublicOnly(Hex.decode(hex))).collect(Collectors.toList());

        // Multisig address created in bitcoind with the following private keys:
        // 47129ffed2c0273c75d21bb8ba020073bb9a1638df0e04853407461fdd9e8b83
        // 9f72d27ba603cfab5a0201974a6783ca2476ec3d6b4e2625282c682e0e5f1c35
        // e1b17fcd0ef1942465eee61b20561b16750191143d365e71de08b33dd84a9788
        oldFederationAddress = "2N7ZgQyhFKm17RbaLqygYbS7KLrQfapyZzu";

        minSecondsBetweenCallsReceiveHeader = 300;  // 5 minutes in seconds
        maxDepthBlockchainAccepted = 25;

        minimumPegoutValuePercentageToReceiveAfterFee = 80;
    }

    public static BridgeTestNetConstants getInstance() {
        return instance;
    }

}
