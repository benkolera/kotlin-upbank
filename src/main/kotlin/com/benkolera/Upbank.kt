package com.benkolera

import com.benkolera.upbank.util.Ktor
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.time.OffsetDateTime

enum class AccountType{
    SAVER, TRANSACTIONAL
}

enum class OwnershipType {
    INDIVIDUAL, JOINT
}

data class Money(
    val currencyCode: String,
    val value: String,
    val valueInBaseUnits: Long
)

data class AccountAttributes(
    val displayName: String,
    val accountType: AccountType,
    val ownershipType: OwnershipType,
    val balance: Money,
    val createdAt: OffsetDateTime
)

data class LinksRelated(
    val related: URL
)

data class AccountTransactionRel(
    val links: LinksRelated
)

data class AccountRels(
    val transactions: AccountTransactionRel?
)

data class LinksSelf(
    val self: URL
)

data class LinksSelfRelated(
    val self: URL,
    val related: URL?
)

@JsonIgnoreProperties("type")
data class Account(
    val id: String,
    val attributes: AccountAttributes,
    val relationships: AccountRels,
    val links: LinksSelf?
)

enum class TransactionStatus {
    HELD, SETTLED
}

data class HoldInfo(
    val amount: Money,
    val foreignAmount: Money?
)

data class RoundUp(
    val amount: Money,
    val boostPortion: Money?
)

data class Cashback(
    val description: String,
    val amount: Money
)

data class TransactionAttributes(
    val status: TransactionStatus,
    val rawText: String?,
    val description: String,
    val message: String?,
    @JsonProperty("isCategorizable") val isCategorizable: Boolean,
    val holdInfo: HoldInfo?,
    val roundUp: RoundUp?,
    val cashback: Cashback?,
    val amount: Money,
    val foreignAmount: Money?,
    val settledAt: OffsetDateTime?,
    val createdAt: OffsetDateTime
)

data class DataEntity(
    val id: String,
    val type: String
)

data class TransactionAccountRel(
    val data: DataEntity,
    val links: LinksRelated?
)

data class TransactionTransferAccountRel(
    val data: DataEntity?,
    val links: LinksRelated?
)

data class TransactionParentCategoryRel(
    val data: DataEntity?,
    val links: LinksRelated?
)


data class TransactionsTagsRel(
    val data: List<DataEntity>?,
    val links: LinksSelf?
)

data class TransactionCategoryRel(
    val data: DataEntity?,
    val links: LinksSelfRelated?
)

data class TransactionRels(
    val account: TransactionAccountRel,
    val transferAccount: TransactionTransferAccountRel?,
    val category: TransactionCategoryRel,
    val parentCategory: TransactionParentCategoryRel,
    val tags: TransactionsTagsRel
)

@JsonIgnoreProperties("type")
data class Transaction(
    val id: String,
    val attributes: TransactionAttributes,
    val relationships: TransactionRels,
    val links: LinksSelf?
)

data class LinksPaginated(
    val prev: URL?,
    val next: URL?
)

data class PaginatedList<T>(
    val data: List<T>,
    val links: LinksPaginated
)

class Upbank(val accessToken: String, val ktor: HttpClient = Ktor.prodClient()) {
    val baseUri = Url("https://api.up.com.au/api/v1")
    fun accounts(): List<Account> {
        return runBlocking { getPaginatedListRecursive("$baseUri/accounts") }
    }
    fun transactionsSince(cutoff: OffsetDateTime): List<Transaction> {
        return runBlocking { getPaginatedListRecursive(
            baseUri.copy(
                encodedPath = baseUri.encodedPath + "/transactions",
                parameters = parametersOf(
                    Pair("filter[since]", listOf(cutoff.toString()))
                )
            ).toString(),
        )}
    }

    private suspend inline fun <reified T> getPaginatedListRecursive(uri: String): List<T> {
        return getPaginatedListRecursiveUnsafe(uri, typeInfo<PaginatedList<T>>())
    }

    private suspend fun <T> getPaginatedListRecursiveUnsafe(uri: String, typeInfo: TypeInfo, out: List<T> = listOf()):List<T> {
        // The ktor functions having reified type params means something silently breaks if you try
        // to ask for ktor.get<PaginatedList<T>> . It actually returns you a list of hashmaps and your
        // code will crash later because something that you think ought to be an account is not.
        //
        // This makes sense because generics are erased, so the underlying reflection code in ktor-client-jackson gets
        // a PaginatedList<T> and it has no way to find out what the T is, and unhelpfully defaults to a hashmap of
        // string to string. All serialisation plugins are going to have the same problem, but maybe gson or
        // kotlin-serialization would crash rather than doing something very dodgy and confusing.
        //
        // It is annoying that there is not a compiler warning here warning you that a bad thing probably will happen
        // given you are calling an inline function with a reified type param from a function where the type param is
        // not reified.
        //
        // And of course we can't reify T here because reifying means inlining and you can't have an inline recursive
        // function even if it's tail recursive. Sadness. But we can easily fix this in a non recursive inline function
        // that calls this very unsafe function. See getPaginatedListRecursive :)
        //
        // This makes me miss haskell type classes 😭

        val httpRes: HttpResponse = ktor.get(uri){
            header("Authorization", "Bearer $accessToken")
        }
        // This is busted and returns the hashmap val res = httpRes.receive<PaginatedList<T>>()
        val res = httpRes.call.receive(typeInfo) as PaginatedList<T>
        val newOut: List<T> = listOf(out,res.data).flatten()
        return if (res.links.next != null)
            getPaginatedListRecursiveUnsafe(res.links.next.toString(), typeInfo, newOut)
        else
            newOut

    }
}
