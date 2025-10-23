package app.perawallet.walletconnectv2.internal.common.explorer

import androidx.core.net.toUri
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.App
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Colors
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.DappListings
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Desktop
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.ImageUrl
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Injected
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Listing
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Metadata
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Mobile
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.NotificationType
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.NotifyConfig
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Project
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.ProjectListing
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.SupportedStandard
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.ExplorerService
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.AppDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.ColorsDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.DappListingsDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.DesktopDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.ImageUrlDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.InjectedDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.ListingDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.MetadataDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.MobileDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.NotificationTypeDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.NotifyConfigDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.ProjectDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.ProjectListingDTO
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.model.SupportedStandardDTO
import app.perawallet.walletconnectv2.internal.common.model.ProjectId

//discuss: Repository could be inside domain
class ExplorerRepository(
    private val explorerService: ExplorerService,
    private val projectId: ProjectId,
) {

    suspend fun getAllDapps(): DappListings {
        return with(explorerService.getAllDapps(projectId.value)) {
            if (isSuccessful && body() != null) {
                body()!!.toDappListing()
            } else {
                throw Throwable(errorBody()?.string())
            }
        }
    }

    suspend fun getProjects(
        page: Int,
        entries: Int,
        isVerified: Boolean,
        isFeatured: Boolean,
    ): ProjectListing {
        return with(explorerService.getProjects(projectId.value, entries, page, isVerified, isFeatured)) {
            if (isSuccessful && body() != null) {
                body()!!.toProjectListing()
            } else {
                throw Throwable(errorBody()?.string())
            }
        }
    }

    suspend fun getNotifyConfig(
        appDomain: String,
    ): NotifyConfig {
        return with(explorerService.getNotifyConfig(projectId.value, appDomain)) {
            if (isSuccessful && body() != null) {
                body()!!.toNotifyConfig()
            } else {
                throw Throwable(errorBody()?.string())
            }
        }
    }

    private fun NotifyConfigDTO.toNotifyConfig(): NotifyConfig {
        return with(data) {
            NotifyConfig(
                types = notificationTypes.map { it.toNotificationType() },
                name = name,
                description = description,
                imageUrl = imageUrl?.toImageUrl(),
                homepage = homepage ?: "",
                dappUrl = dappUrl,
                isVerified = isVerified,
            )
        }
    }

    private fun NotificationTypeDTO.toNotificationType(): NotificationType = NotificationType(name = name, id = id, description = description, imageUrl = imageUrl?.toImageUrl())


    private fun ProjectListingDTO.toProjectListing(): ProjectListing {
        return ProjectListing(
            projects = projects.values.map { it.toProject() },
            count = count,
        )
    }

    private fun ProjectDTO.toProject(): Project = Project(
        id = id,
        name = name?.takeIf { it.isNotBlank() } ?: "Name not provided",
        description = description?.takeIf { it.isNotBlank() } ?: "Description not provided",
        homepage = homepage?.takeIf { it.isNotBlank() } ?: "Homepage not provided",
        imageId = imageId?.takeIf { it.isNotBlank() } ?: "ImageID not provided",
        imageUrl = imageUrl?.toImageUrl() ?: ImageUrl("", "", ""),
        dappUrl = dappUrl?.takeIf { it.isNotBlank() } ?: "Dapp url not provided",
        order = order,
    )

    private fun DappListingsDTO.toDappListing(): DappListings {
        return DappListings(
            listings = listings.values.map { it.toListing() }, count = count, total = total
        )
    }

    private fun ListingDTO.toListing(): Listing = Listing(
        id = id,
        name = name,
        description = description,
        homepage = homepage.toUri(),
        chains = chains,
        versions = versions,
        sdks = sdks,
        appType = appType,
        imageId = imageId,
        imageUrl = imageUrl.toImageUrl(),
        app = app.toApp(),
        injected = injected?.map { it.toInjected() },
        mobile = mobile.toMobile(),
        desktop = desktop.toDesktop(),
        supportedStandards = supportedStandards.map { it.toSupportedStandard() },
        metadata = metadata.toMetadata(),
        updatedAt = updatedAt
    )

    private fun ImageUrlDTO.toImageUrl(): ImageUrl = ImageUrl(
        sm = sm,
        md = md,
        lg = lg,
    )

    private fun AppDTO.toApp(): App = App(
        browser = browser, ios = ios, android = android, mac = mac, windows = windows, linux = linux, chrome = chrome, firefox = firefox, safari = safari, edge = edge, opera = opera
    )

    private fun InjectedDTO.toInjected(): Injected = Injected(
        namespace = namespace, injectedId = injectedId
    )

    private fun MobileDTO.toMobile(): Mobile = Mobile(
        native = native,
        universal = universal,
    )

    private fun DesktopDTO.toDesktop(): Desktop = Desktop(
        native = native,
        universal = universal,
    )

    private fun SupportedStandardDTO.toSupportedStandard(): SupportedStandard = SupportedStandard(
        id = id, url = url, title = title, standardId = standardId, standardPrefix = standardPrefix
    )

    private fun MetadataDTO.toMetadata(): Metadata = Metadata(
        shortName = shortName,
        colors = colors.toColors(),
    )

    private fun ColorsDTO.toColors(): Colors = Colors(
        primary = primary, secondary = secondary
    )
}