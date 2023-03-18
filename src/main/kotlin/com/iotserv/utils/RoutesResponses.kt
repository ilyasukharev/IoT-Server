package com.iotserv.utils

object RoutesResponses {
    const val authorizationHasBeenCompleted = "Authorization has been completed"
    const val verifyCodeWasSent = "Verify code was sent"
    const val sendingEmailWithVerifyCode = "Sending email with verify code"
    const val successfullyRegistered = "Successfully registered"
    const val clientConnected = "Client connected"
    const val attemptToConnect2Sides = "Attempt to connect two sides"
    const val boardConnected = "Board connected"
    const val codeIsRight = "Code is right"
    const val codeIsWrong = "Code is wrong"
    const val passwordWasSuccessfullyChanged = "Password was successfully changed"
    const val dataWasSuccessfullyChanged = "Data was successfully changed"
    const val incorrectEmailFormat = "Incorrect email format"
    const val incorrectEmailLength = "Incorrect email length"
    const val incorrectPasswordLength = "Incorrect password length"
    const val incorrectCodeLength = "Incorrect code length"
    const val incorrectCodeFormat = "Incorrect code format"
    const val successfullyBoardConnection = "Successfully board connection"
    const val sendingSettingsAccepted = "Sending settings accepted"
    const val settingsSuccessfullyReceived = "Settings successfully received"
    const val unknownSocketCommand = "Unknown socket command"
    const val boardWasFound = "Board was found"
    const val dataWereSuccessfullyWrote = "Data were successfully wrote"
    const val arrivedSettingsIsIncorrect = "Arrived settings is incorrect"
    const val loggerFileIsNotExists = "Logger file is not exists"
    const val loggerCanNotLogging = "Logger can not logging"
    const val sendingBoardIDAccepted = "Sending board id accepted"
    const val boardIDSuccessfullyReceived = "Board id successfully received"
    const val arrivedBoardIDIsIncorrect = "Arrived board id is incorrect"
    const val serializationFailed = "Serialization failed"
    const val searchingTheClient = "Searching the client"
    const val listWasSent = "List was sent"
    const val deviceDataWasSent = "Device data was sent"
    const val deviceStateHasBeenUpdated = "Device state has been updated"
    const val updateIsNull = "Update is null"
    const val accessTokenWasUpdated = "Access token was updated"

    //Exceptions

    //Authorization [EA]
    const val confirmCodeIsNotRightCode = "EA01"
    const val connectionTimeWasUpCode = "EA02"
    const val userAlreadyExistsCode = "EA03"
    const val clientIsNotAuthenticatedCode = "EA04"
    const val userNotFoundOrPasswordIsIncorrectCode = "EA05"

    const val confirmCodeIsNotRight = "Confirm code is not right"
    const val connectionTimeWasUp = "Connection time to confirm code was up"
    const val userAlreadyExists = "User already exists"
    const val clientIsNotAuthenticated = "Client is not authenticated"
    const val userNotFoundOrPasswordIsIncorrect = "User not found or password is incorrect"

    //Exposed [EE]
    const val deviceWasNotFoundCode = "EE01"
    const val sensorWasNotFoundCode = "EE02"
    const val userNotFoundCode = "EE03"
    const val userOrDeviceNotFoundCode = "EE04"
    const val suchBoardUUIDAlreadyExistsCode = "EE05"
    const val deviceStateHasNotBeenUpdatedCode = "EE06"
    const val suchBoardUUIDIsNotExistsCode = "EE07"

    const val deviceWasNotFound = "Device was not found"
    const val sensorWasNotFound = "Sensor type was not found"
    const val userNotFound = "User not found"
    const val userOrDeviceNotFound = "User or device not found"
    const val suchBoardUUIDAlreadyExists = "Such board UUID already exists"
    const val deviceStateHasNotBeenUpdated = "Device state has not been updated"
    const val suchBoardUUIDIsNotExists = "Such board UUID is not exists"

    //Token [ET]
    const val tokenIsNotValidOrHasExpiredCode = "EA01"
    const val uuidWasNotFoundCode = "EA02"
    const val tokenTypeIsMissingCode = "EA03"

    const val tokenIsNotValidOrHasExpired = "Token is not valid or has expired"
    const val uuidWasNotFound = "UUID was not found"
    const val tokenTypeIsMissing = "Token type is missing"

    //MailDeliverException [EMD]
    const val messageAlreadyBuiltCode = "EMD01"
    const val messageSendingExceptionCode = "EMD02"

    const val messageAlreadyBuilt = "Message already built"
    const val messageSendingException = "Message sending exception"

    //SocketException
    const val commandIsUnknown = "Command is unknown"
    const val suchBoardAlreadyListening = "Such board already listening"
    const val clientsWasNotConnected = "Clients was not connected"
    const val boardIsNotVerifiedOrUserNotFound = "Board is not verified or user not found"
    const val suchDeviceAlreadyRegisteredByUser = "Such device already registered by user"
    const val socketTimeoutResponse = "Socket timeout response"
    const val suchBoardIsListening = "Such board is listening"


    //OtherException [OE]
    const val deviceIsNotListeningCode = "OE01"
    const val arrivedStateOfTypeIsNotCorrectCode = "OE02"

    const val deviceIsNotListening = "Device is not listening"
    const val arrivedStateOfTypeIsNotCorrect = "Arrived state of type is not correct"



}