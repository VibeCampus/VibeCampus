import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useUserStore } from './user'
import { normalizePost, normalizeUser, parseListPayload } from '@/api/normalize'

const mockUsers = [
  {
    id: 1,
    username: '测试同学',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=240&q=80',
    phone: '138****1234',
    email: 'demo@campus.com',
    bio: '热爱分享的大三学生，常在图书馆和操场出没。',
    gender: '男',
    major: '软件工程',
    joinedAt: '2024-09',
  },
  {
    id: 2,
    username: '张同学',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=240&q=80',
    phone: '139****2211',
    email: 'zhang@campus.com',
    bio: '喜欢摄影和记录校园日常。',
    gender: '女',
    major: '新闻传播',
    joinedAt: '2023-09',
  },
  {
    id: 3,
    username: '李同学',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=240&q=80',
    phone: '137****5422',
    email: 'li@campus.com',
    bio: '备考中，愿意免费分享复习资料。',
    gender: '男',
    major: '电子信息工程',
    joinedAt: '2022-09',
  },
  {
    id: 4,
    username: '王同学',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=240&q=80',
    phone: '136****9321',
    email: 'wang@campus.com',
    bio: '热衷于校园活动和篮球。',
    gender: '男',
    major: '体育教育',
    joinedAt: '2022-09',
  },
  {
    id: 5,
    username: '陈同学',
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=240&q=80',
    phone: '135****6178',
    email: 'chen@campus.com',
    bio: '经常出没于食堂和创业园。',
    gender: '女',
    major: '市场营销',
    joinedAt: '2023-03',
  },
  {
    id: 6,
    username: '刘同学',
    avatar: 'https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=240&q=80',
    phone: '134****4791',
    email: 'liu@campus.com',
    bio: '跑步爱好者，也喜欢分享学习心得。',
    gender: '女',
    major: '数学与应用数学',
    joinedAt: '2021-09',
  },
  {
    id: 7,
    username: '周同学',
    avatar: 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=240&q=80',
    phone: '133****2238',
    email: 'zhou@campus.com',
    bio: '本地通，最懂学校附近美食。',
    gender: '男',
    major: '旅游管理',
    joinedAt: '2020-09',
  },
  {
    id: 8,
    username: '吴同学',
    avatar: 'https://images.unsplash.com/photo-1504257432389-52343af06ae3?auto=format&fit=crop&w=240&q=80',
    phone: '132****8801',
    email: 'wu@campus.com',
    bio: '数码设备爱好者，常年混迹二手区。',
    gender: '保密',
    major: '计算机科学',
    joinedAt: '2021-09',
  },
]

const mockPosts = [
  {
    id: 1,
    authorId: 2,
    category: 'social_find',
    anonymous: false,
    content: '图书馆三楼，戴眼镜的男同学，你昨天帮我捡了书包，谢谢你！想认识你，能加个微信吗？',
    time: '5分钟前',
    likes: 128,
    comments: 34,
    images: [],
  },
  {
    id: 2,
    authorId: 3,
    category: 'share',
    anonymous: false,
    content: '整理了一份考研数学真题解析，包括近五年的完整解题过程，有需要的同学私信我，免费分享！',
    time: '23分钟前',
    likes: 342,
    comments: 67,
    images: ['https://picsum.photos/seed/post2a/300/200', 'https://picsum.photos/seed/post2b/300/200'],
  },
  {
    id: 3,
    authorId: 4,
    category: 'trade',
    anonymous: false,
    content: '出二手高数教材，同济第七版，八成新，无乱涂，原价35，现15出，取货地点一食堂门口，不接受砍价。',
    time: '1小时前',
    likes: 56,
    comments: 12,
    images: [],
  },
  {
    id: 4,
    authorId: null,
    category: 'general',
    anonymous: true,
    content: '宿舍的室友每天晚上打游戏开麦到凌晨两三点，已经影响我睡眠两个月了，跟他说了也没用，求大家支招怎么办？',
    time: '2小时前',
    likes: 891,
    comments: 213,
    images: [],
  },
  {
    id: 5,
    authorId: 5,
    category: 'share',
    anonymous: false,
    content: '今天实习的公司突然通知下周起全员居家办公，这种事情发生后我发现其实大家都不太适应，分享几个高效在家办公的方法给大家。',
    time: '3小时前',
    likes: 234,
    comments: 45,
    images: ['https://picsum.photos/seed/post5/400/250'],
  },
  {
    id: 6,
    authorId: 6,
    category: 'social_love',
    anonymous: false,
    content: '在操场跑步的时候遇见一个女孩子，她跑完步之后喜欢坐在台阶上听歌，如果你看到这条帖子，我想认识你。',
    time: '4小时前',
    likes: 1204,
    comments: 89,
    images: [],
  },
  {
    id: 7,
    authorId: 7,
    category: 'general',
    anonymous: false,
    content: '求推荐学校附近好吃不贵的饭馆？最好步行10分钟以内，预算人均30以内，口味不限。',
    time: '5小时前',
    likes: 445,
    comments: 127,
    images: [],
  },
  {
    id: 8,
    authorId: 8,
    category: 'trade',
    anonymous: false,
    content: '转让一台八成新MacBook Air M1，16G+256G，使用不到一年，因换了新电脑，价格5800，有意者私聊。',
    time: '6小时前',
    likes: 78,
    comments: 31,
    images: [
      'https://picsum.photos/seed/post8a/300/200',
      'https://picsum.photos/seed/post8b/300/200',
      'https://picsum.photos/seed/post8c/300/200',
      'https://picsum.photos/seed/post8d/300/200',
    ],
  },
  {
    id: 9,
    authorId: 5,
    category: 'social_buddy',
    anonymous: false,
    content: '有没有一起夜跑的朋友？每晚操场8点，配速6分左右，能坚持的来组个队，男女都行。',
    time: '7小时前',
    likes: 56,
    comments: 18,
    images: [],
  },
]

const mockComments = {
  2: [
    {
      id: 1,
      authorId: 4,
      time: '1小时前',
      content: '太感谢了，正好需要这些资料！',
      likes: 12,
      replies: [
        { id: 11, authorId: 3, time: '50分钟前', content: '已私信你了，注意查收~', likes: 3 },
      ],
    },
    {
      id: 2,
      authorId: 5,
      time: '2小时前',
      content: '请问有没有专业课资料？',
      likes: 5,
      replies: [],
    },
    {
      id: 3,
      authorId: 2,
      time: '3小时前',
      content: '楼主辛苦了，收藏了！',
      likes: 8,
      replies: [],
    },
  ],
  7: [
    {
      id: 4,
      authorId: 1,
      time: '30分钟前',
      content: '东门口那家砂锅和鸡公煲都不错，性价比很高。',
      likes: 2,
      replies: [],
    },
  ],
}

const mockFavorites = {
  1: [5, 7],
}

const mockFollowing = {
  1: [3, 5, 7],
  2: [1],
  3: [1, 4],
  4: [1],
  5: [3],
  6: [1],
  7: [1, 5],
  8: [],
}

function clone(data) {
  return JSON.parse(JSON.stringify(data))
}

function loadState(key, fallback) {
  const saved = localStorage.getItem(key)
  if (!saved) return clone(fallback)
  try {
    return JSON.parse(saved)
  } catch {
    return clone(fallback)
  }
}

function normalizeId(value) {
  return Number(value)
}

export const useSocialStore = defineStore('social', () => {
  const userStore = useUserStore()
  const users = ref(loadState('social-users', mockUsers))
  const posts = ref(loadState('social-posts', mockPosts))
  const commentsByPostId = ref(loadState('social-comments', mockComments))
  const favoritesByUserId = ref(loadState('social-favorites', mockFavorites))
  const followingByUserId = ref(loadState('social-following', mockFollowing))

  const currentUserId = computed(() => {
    const id = userStore.userInfo?.id
    return id != null ? normalizeId(id) : 0
  })

  function findUserById(userId) {
    return users.value.find(user => user.id === normalizeId(userId)) || null
  }

  const currentUser = computed(() => {
    const id = currentUserId.value
    if (!id) {
      return null
    }
    return (
      findUserById(id) || normalizeUser(userStore.userInfo) || null
    )
  })

  function getFavoriteIds(userId) {
    return favoritesByUserId.value[normalizeId(userId)] || []
  }

  function isFollowing(targetUserId) {
    const following = followingByUserId.value[currentUserId.value] || []
    return following.includes(normalizeId(targetUserId))
  }

  function enrichPost(post) {
    const favoriteIds = getFavoriteIds(currentUserId.value)
    const author =
      post.author ||
      (post.authorId ? findUserById(post.authorId) : null) ||
      null
    return {
      ...post,
      author,
      favorited: favoriteIds.includes(post.id),
    }
  }

  const feedPosts = computed(() => posts.value.map(enrichPost))

  function getPostById(postId) {
    const post = posts.value.find(item => item.id === normalizeId(postId))
    return post ? enrichPost(post) : null
  }

  function getCommentsByPostId(postId) {
    return (commentsByPostId.value[normalizeId(postId)] || []).map(comment => ({
      ...comment,
      author: findUserById(comment.authorId),
      replies: (comment.replies || []).map(reply => ({
        ...reply,
        author: findUserById(reply.authorId),
      })),
    }))
  }

  function getUserPosts(userId) {
    return posts.value
      .filter(post => post.authorId === normalizeId(userId))
      .map(enrichPost)
  }

  function getUserComments(userId) {
    return Object.entries(commentsByPostId.value).flatMap(([postId, list]) =>
      list
        .filter(comment => comment.authorId === normalizeId(userId))
        .map(comment => {
          const post = getPostById(postId)
          return {
            id: comment.id,
            postId: normalizeId(postId),
            postTitle: post?.content || '帖子已删除',
            content: comment.content,
            time: comment.time,
          }
        }),
    )
  }

  function getFavoritePosts(userId) {
    return getFavoriteIds(userId)
      .map(postId => getPostById(postId))
      .filter(Boolean)
  }

  function getFollowing(userId) {
    return (followingByUserId.value[normalizeId(userId)] || [])
      .map(id => findUserById(id))
      .filter(Boolean)
  }

  function getFollowers(userId) {
    const targetId = normalizeId(userId)
    return users.value.filter(user => (followingByUserId.value[user.id] || []).includes(targetId))
  }

  function getUserStats(userId) {
    return {
      postCount: getUserPosts(userId).length,
      followingCount: getFollowing(userId).length,
      followerCount: getFollowers(userId).length,
      favoriteCount: getFavoritePosts(userId).length,
    }
  }

  function upsertUser(user) {
    const u = normalizeUser(user)
    if (!u?.id) return
    const id = normalizeId(u.id)
    const i = users.value.findIndex(x => normalizeId(x.id) === id)
    if (i >= 0) {
      Object.assign(users.value[i], u)
    } else {
      users.value.push(u)
    }
  }

  function syncUserFromProfile(user) {
    upsertUser(user)
  }

  function setFeedFromServer(rawList) {
    const arr = parseListPayload(rawList)
    const list = arr.map(p => normalizePost(p)).filter(Boolean)
    for (const p of list) {
      if (p.author) {
        upsertUser(p.author)
      }
    }
    posts.value = list
  }

  function upsertPostFromServer(raw) {
    const p = normalizePost(raw)
    if (!p) return null
    if (p.author) {
      upsertUser(p.author)
    }
    const i = posts.value.findIndex(x => x.id === p.id)
    if (i >= 0) {
      posts.value[i] = p
    } else {
      posts.value.unshift(p)
    }
    return p
  }

  function updateCurrentUserProfile(patch) {
    const user = findUserById(currentUserId.value)
    if (!user) return
    Object.assign(user, patch)
  }

  function toggleFollow(targetUserId) {
    const targetId = normalizeId(targetUserId)
    if (!targetId || targetId === currentUserId.value) return false
    const list = followingByUserId.value[currentUserId.value] || []
    const exists = list.includes(targetId)
    followingByUserId.value[currentUserId.value] = exists
      ? list.filter(id => id !== targetId)
      : [...list, targetId]
    return !exists
  }

  function addComment(postId, content) {
    const id = normalizeId(postId)
    const nextComment = {
      id: Date.now(),
      authorId: currentUserId.value,
      time: '刚刚',
      content,
      likes: 0,
      replies: [],
    }
    const list = commentsByPostId.value[id] || []
    commentsByPostId.value[id] = [nextComment, ...list]
    const post = posts.value.find(item => item.id === id)
    if (post) post.comments += 1
  }

  function addReply(postId, commentId, content) {
    const id = normalizeId(postId)
    const target = (commentsByPostId.value[id] || []).find(comment => comment.id === normalizeId(commentId))
    if (!target) return
    target.replies.push({
      id: Date.now(),
      authorId: currentUserId.value,
      time: '刚刚',
      content,
      likes: 0,
    })
  }

  const hotPosts = computed(() =>
    [...feedPosts.value]
      .sort((a, b) => b.likes + b.comments * 10 - (a.likes + a.comments * 10))
      .slice(0, 10)
      .map(post => ({
        id: post.id,
        title: post.content,
        heat: post.likes * 10 + post.comments * 25,
      })),
  )

  watch(users, value => {
    localStorage.setItem('social-users', JSON.stringify(value))
  }, { deep: true })

  watch(posts, value => {
    localStorage.setItem('social-posts', JSON.stringify(value))
  }, { deep: true })

  watch(commentsByPostId, value => {
    localStorage.setItem('social-comments', JSON.stringify(value))
  }, { deep: true })

  watch(favoritesByUserId, value => {
    localStorage.setItem('social-favorites', JSON.stringify(value))
  }, { deep: true })

  watch(followingByUserId, value => {
    localStorage.setItem('social-following', JSON.stringify(value))
  }, { deep: true })

  return {
    users,
    posts,
    currentUser,
    currentUserId,
    feedPosts,
    hotPosts,
    findUserById,
    getPostById,
    getCommentsByPostId,
    getUserPosts,
    getUserComments,
    getFavoritePosts,
    getFollowing,
    getFollowers,
    getUserStats,
    isFollowing,
    toggleFollow,
    updateCurrentUserProfile,
    addComment,
    addReply,
    syncUserFromProfile,
    setFeedFromServer,
    upsertPostFromServer,
  }
})
