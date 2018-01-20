package com.example.rkinabhi.merchack;

/**
 * Created by rkinabhi on 19-01-2018.
 */

public enum CarState {
    AtOrigin,
    AtDestination,
    ChargeDefecient,
    ChargeBelowThresholdButTravelling,
    WaitingForResponse,
    RequestingCharge,
    Charging,
    Responding,
    TravellingToPrimaryDestination,
    TravellingToSecondaryDestination,
    TransferringCharge
}
