<template>

<v-data-table
    :headers="headers"
    :items="myPage"
    :items-per-page="5"
    class="elevation-1"
  ></v-data-table>

</template>

<script>
  const axios = require('axios').default;

  export default {
    name: 'MyPage',
    props: {
      value: Object,
      editMode: Boolean,
      isNew: Boolean
    },
    data: () => ({
        headers: [
            { text: "id", value: "id" },
            { text: "bookId", value: "bookId" },
            { text: "hostId", value: "hostId" },
            { text: "price", value: "price" },
            { text: "startDate", value: "startDate" },
            { text: "endDate", value: "endDate" },
            { text: "roomId", value: "roomId" },
            { text: "status", value: "status" },
            { text: "payId", value: "payId" },
            { text: "guestId", value: "guestId" },
        ],
        myPage : [],
    }),
    async created() {
      var temp = await axios.get(axios.backend + '/mypages')

      this.myPage = temp.data._embedded.mypages;

    },
    methods: {
    }
  }
</script>

