package com.bytedance.xhsdemo.data

import android.os.Handler
import android.os.Looper
import com.bytedance.xhsdemo.model.Comment
import com.bytedance.xhsdemo.model.Post
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random

data class PageResult(val items: List<Post>, val hasMore: Boolean)

object FakePostRepository {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())

    private const val INITIAL_COUNT = 20
    private const val TOTAL_COUNT = 100

    private val photos = listOf(
        "https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1523419400528-406f99c95694?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=1200&q=80",
        "https://images.unsplash.com/photo-1492724441997-5dc865305da7?auto=format&fit=crop&w=1200&q=80"
    )
    private val avatars = listOf(
        "https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=200&q=60",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=60",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=60",
        "https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=200&q=60",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=60"
    )
    private val titles = listOf(
        "周末去海边度假，一起看最美的晚霞",
        "家里的小阳台也能拍出杂志感",
        "城市里的露营地，夜晚星河超治愈",
        "快闪咖啡馆探店，设计感拉满",
        "秋日山间徒步，随手一拍都是壁纸",
        "夏天的市集，色彩和味道都很热烈",
        "在厨房里慢下来，做一顿有烟火气的饭",
        "和朋友的周末小聚，记录下这些笑脸"
    )

    private val posts = mutableListOf<Post>()

    init {
        generatePosts(INITIAL_COUNT)
    }

    fun fetchPosts(page: Int, pageSize: Int, callback: (Result<PageResult>) -> Unit) {
        mainHandler.postDelayed({
            val shouldFail = page > 1 && random.nextFloat() < 0.15f
            if (shouldFail) {
                callback(Result.failure(IllegalStateException("网络开小差了，重试试试看")))
                return@postDelayed
            }
            ensureSize(min(page * pageSize + 4, TOTAL_COUNT))
            val start = (page - 1) * pageSize
            val end = min(posts.size, start + pageSize)
            val data = if (start in 0 until end) {
                posts.subList(start, end).map { it.copy(comments = it.comments.toList()) }
            } else {
                emptyList()
            }
            val hasMore = end < min(posts.size, TOTAL_COUNT)
            callback(Result.success(PageResult(data, hasMore)))
        }, 520)
    }

    fun addPost(post: Post) {
        posts.add(0, post.copy(publishTime = "刚刚"))
    }

    fun findPost(id: String): Post? = posts.firstOrNull { it.id == id }

    fun refreshData() {
        posts.clear()
        generatePosts(INITIAL_COUNT)
    }

    private fun ensureSize(target: Int) {
        if (posts.size < target) {
            generatePosts(target - posts.size)
        }
    }

    private fun generatePosts(count: Int) {
        repeat(count) { posts.add(randomPost()) }
    }

    private fun randomPost(): Post {
        val title = titles.random(random)
        val cover = photos.random(random)
        val avatar = avatars.random(random)
        val author = listOf("橙子汽水", "北岛晚风", "晨光画室", "旅居的猫", "森系女孩").random(random)
        val time = listOf("1 小时前", "昨天", "刚刚", "3 小时前", "周末").random(random)
        val like = random.nextInt(30, 500)
        return Post(
            id = UUID.randomUUID().toString(),
            title = title,
            content = "「$title」\n\n碎片化记录日常，用照片和文字留住一刻心情。简单的布置、真实的光影、还有身边的小确幸，都在这一篇里。",
            imageUrl = cover,
            authorName = author,
            authorAvatar = avatar,
            publishTime = time,
            likes = like,
            comments = randomComments()
        )
    }

    private fun randomComments(): List<Comment> {
        val sample = listOf(
            "好会生活，想去同款地点！",
            "色彩太治愈了，点赞！",
            "求拍摄参数～",
            "看完就想立刻出发旅行呢",
            "这也太会拍了！"
        )
        val count = random.nextInt(1, 4)
        return (0 until count).map {
            Comment(
                id = UUID.randomUUID().toString(),
                userName = listOf("旅人A", "小丸子", "南山", "Coco").random(random),
                userAvatar = avatars.random(random),
                content = sample.random(random)
            )
        }
    }
}
