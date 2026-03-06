package com.atelbay.money_manager.core.auth

class SignInCancelledException : Exception("Google Sign-In was cancelled by the user")
class SignInFailedException(cause: Throwable) : Exception("Google Sign-In failed", cause)
